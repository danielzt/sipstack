package io.sipstack.transaction.impl;

import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.actor.ActorSupport;
import io.sipstack.actor.Cancellable;
import io.sipstack.config.TimersConfiguration;
import io.sipstack.config.TransactionLayerConfiguration;
import io.sipstack.core.Utils;
import io.sipstack.event.Event;
import io.sipstack.netty.codec.sip.SipTimer;
import io.sipstack.transaction.TransactionId;
import io.sipstack.transaction.TransactionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 *
 * Implements the Invite Client Transaction state machine as specified by rfc6026.
 *
 * <pre>
 *
 *
 *                                  |INVITE from TU
 *                Timer A fires     |INVITE sent      Timer B fires
 *                Reset A,          V                 or Transport Err.
 *                INVITE sent +-----------+           inform TU
 *                  +---------|           |--------------------------+
 *                  |         |  Calling  |                          |
 *                  +-------->|           |-----------+              |
 * 300-699                    +-----------+ 2xx       |              |
 * ACK sent                      |  |       2xx to TU |              |
 * resp. to TU                   |  |1xx              |              |
 * +-----------------------------+  |1xx to TU        |              |
 * |                                |                 |              |
 * |                1xx             V                 |              |
 * |                1xx to TU +-----------+           |              |
 * |                +---------|           |           |              |
 * |                |         |Proceeding |           |              |
 * |                +-------->|           |           |              |
 * |                          +-----------+ 2xx       |              |
 * |         300-699             |    |     2xx to TU |              |
 * |         ACK sent,  +--------+    +---------------+              |
 * |         resp. to TU|                             |              |
 * |                    |                             |              |
 * |                    V                             V              |
 * |              +-----------+                   +----------+       |
 * +------------->|           |Transport Err.     |          |       |
 *                | Completed |Inform TU          | Accepted |       |
 *             +--|           |-------+           |          |-+     |
 *     300-699 |  +-----------+       |           +----------+ |     |
 *     ACK sent|    ^  |              |               |  ^     |     |
 *             |    |  |              |               |  |     |     |
 *             +----+  |              |               |  +-----+     |
 *                     |Timer D fires |  Timer M fires|    2xx       |
 *                     |-             |             - |    2xx to TU |
 *                     +--------+     |   +-----------+              |
 *                              V     V   V                          |
 *                             +------------+                        |
 *                             |            |                        |
 *                             | Terminated |<-----------------------+
 *                             |            |
 *                             +------------+
 *
 *
 * </pre>
 * @author jonas@jonasborjesson.com
 */
public class InviteClientTransactionActor extends ActorSupport<Event, TransactionState> implements TransactionActor {

    private static final Logger logger = LoggerFactory.getLogger(InviteClientTransactionActor.class);

    private final TransactionId id;

    private final SipRequest originalInvite;

    private final TransactionLayerConfiguration config;

    private final TimersConfiguration timersConfig;

    /**
     * When we enter the calling state we will scheduler Timer A (if using unreliable transport)
     * which is our timer for re-transmitting the the INVITE request.
     */
    private Cancellable timerA;

    /**
     * How many times Timer A has fired.
     */
    private int timerACount;

    /**
     * If timer B fires it means that we simply got no responses whatsoever so we will
     * simply transition over to terminated.
     *
     * Timer B is always scheduled no matter if the transport is reliable or unreliable.
     */
    private Cancellable timerB;

    /**
     * Timer D is only scheduled for unreliable transports and is what takes us
     * from COMPLETED to TERMINATED.
     */
    private Cancellable timerD;

    /**
     * Timer M is the timer that takes us from ACCEPTED to TERMINATED
     * and was introduced by RFC 6026
     */
    private Cancellable timerM;

    /**
     * TODO: needs to be passed in.
     */
    private final boolean isUsingUnreliableTransport = true;

    protected InviteClientTransactionActor(final TransactionId id,
                                           final SipRequest invite,
                                           final TransactionLayerConfiguration config) {
        super(id.toString(), TransactionState.INIT, TransactionState.TERMINATED, TransactionState.values());
        this.id = id;
        this.originalInvite = invite;
        this.config = config;
        this.timersConfig = config.getTimers();

        when(TransactionState.INIT, this::onInit);

        when(TransactionState.CALLING, this::onCalling);
        onEnter(TransactionState.CALLING, this::onEnterCalling);
        onExit(TransactionState.CALLING, this::onExitCalling);

        when(TransactionState.PROCEEDING, this::onProceeding);

        when(TransactionState.ACCEPTED, this::onAccepted);
        onEnter(TransactionState.ACCEPTED, this::onEnterAccepted);
        onExit(TransactionState.ACCEPTED, this::onExitAccepted);

        when(TransactionState.COMPLETED, this::onCompleted);
        onExit(TransactionState.COMPLETED, this::onExitCompleted);

        // only need onEnterCompleted if unreliable because otherwise
        // we won't actually schedule Timer D and there is nothing else
        // to do so...
        if (isUsingUnreliableTransport) {
            onEnter(TransactionState.COMPLETED, this::onEnterCompleted);
        }
    }

    public boolean isClientTransaction() {
        return true;
    }

    /**
     * The init state. Just make sure that the first event we receive is the same INVITE as created
     * the transaction (yes, we compare references in this case, that's what we want) and then
     * transition over to the proceeding state.
     */
    private final void onInit(final Event event) {
        if (event.isSipRequestEvent() && event.toSipRequestEvent().request() == originalInvite) {
            ctx().forwardDownstream(event);
        } else {
            logWarn("Bad initial event {}, should have been the same INVITE", event);
        }
        become(TransactionState.CALLING);
    };

    /**
     * Implements the Calling state, which is as follows:
     *
     * <pre>
     *
     *                                  |INVITE from TU
     *                Timer A fires     |INVITE sent      Timer B fires
     *                Reset A,          V                 or Transport Err.
     *                INVITE sent +-----------+           inform TU
     *                  +---------|           |--------------------------+
     *                  |         |  Calling  |                          |
     *                  +-------->|           |-----------+              |
     * 300-699                    +-----------+ 2xx       |              |
     * ACK sent                      |  |       2xx to TU |              |
     * resp. to TU                   |  |1xx              |              |
     *              +----------------+  |1xx to TU        |              |
     *              |                   |                 |              |
     *              V                   V                 V              V
     *      +------------+        +-----------+    +----------+ +------------+
     *      |            |        |           |    |          | |            |
     *      | Completed  |        |Proceeding |    | Accepted | | Terminated |
     *      |            |        |           |    |          | |            |
     *      +------------+        +-----------+    +----------+ +------------+
     *
     * </pre>
     *
     * @param event
     */
    private final void onCalling(final Event event) {
        if (event.isSipResponseEvent()) {
            ctx().forwardUpstream(event);
            final SipResponse response = event.response();
            if (response.isProvisional()) {
                become(TransactionState.PROCEEDING);
            } else if (response.isSuccess()){
                become(TransactionState.ACCEPTED);
            } else {
                ack();
                become(TransactionState.COMPLETED);
            }
        } else if (event.isSipTimerA()) {
            ++timerACount;
            timerA = scheduleTimer(SipTimer.A, calculateNextTimerA());
            retransmitInvite();
        } else if (event.isSipTimerB()) {
            become(TransactionState.TERMINATED);
        } else {
            unhandled(event);
        }
    }

    /**
     * While in the completed state we will re-transmit the final response (which then must be a
     * error response only otherwise we wouldn't be in the completed state to begin with) every time
     * this timer fires.
     *
     * @return
     */
    private Duration calculateNextTimerA(){
        final long defaultTimerA = timersConfig.getTimerA().toMillis();
        final long t2 = timersConfig.getT2().toMillis();
        return Utils.calculateBackoffTimer(timerACount, defaultTimerA, t2);
    }

    private final void onEnterCalling(final Event event) {
        // TODO: At this point I actually don't know if this
        // will be UDP or TCP. Need to figure that one out.
        timerA = scheduleTimer(SipTimer.A, calculateNextTimerA());
        timerB = scheduleTimer(SipTimer.B, timersConfig.getTimerB());
    }

    private final void onExitCalling(final Event event) {
        if (timerA != null) {
            timerA.cancel();
        }
        timerB.cancel();
    }

    /**
     * Implements the Proceeding state, which is as follows:
     *
     * <pre>
     *
     *                          |
     *                          |1xx
     *                          |1xx to TU
     *                          |
     *          1xx             V
     *          1xx to TU +-----------+
     *          +---------|           |
     *          |         |Proceeding |
     *          +-------->|           |
     *                    +-----------+ 2xx
     *   300-699             |    |     2xx to TU
     *   ACK sent,  +--------+    +---------------+
     *   resp. to TU|                             |
     *              |                             |
     *              V                             V
     *        +-----------+                   +----------+
     *        |           |                   |          |
     *        | Completed |                   | Accepted |
     *        |           |                   |          |
     *        +-----------+                   +----------+
     *
     * </pre>
     * @param event
     */
    private final void onProceeding(final Event event) {
        if (event.isSipResponseEvent()) {
            // all responses to TU
            ctx().forwardUpstream(event);

            final SipResponse response = event.response();
            if (response.isSuccess()) {
                become(TransactionState.ACCEPTED);
            } else if (response.isFinal()){
                // error responses since we already checked
                // for success before this
                ack();
                become(TransactionState.COMPLETED);
            }

            // provisional doesn't do anything except
            // pushing the response up to TU, which
            // has already be done. Hence, no else statement
        }
    }

    /**
     * Implements the Accepted state, which is as follows:
     *
     * <pre>
     *                      +----------+
     *                      |          |
     *                      | Accepted |
     *                      |          |-+
     *                      +----------+ |
     *                          |  ^     |
     *                          |  |     |
     *                          |  +-----+
     *             Timer M fires|    2xx
     *                        - |    2xx to TU
     *              +-----------+
     *              V
     *   +------------+
     *   |            |
     *   | Terminated |
     *   |            |
     *   +------------+
     *
     * </pre>
     * @param event
     */
    private final void onAccepted(final Event event) {
        if (event.isSipResponseEvent()) {
            final SipResponse response = event.response();
            if (response.isSuccess()) {
                ctx().forwardUpstream(event);
            }
        } else if (event.isSipTimerM()) {
            become(TransactionState.TERMINATED);
        }
    }

    private final void onEnterAccepted(final Event event) {
        timerM = scheduleTimer(SipTimer.M, timersConfig.getTimerM());
    }

    private final void onExitAccepted(final Event event) {
        timerM.cancel();
    }

    /**
     *
     * Implements the completed state as follows:
     *
     * <pre>
     *
     *                +-----------+
     *                |           |Transport Err.
     *                | Completed |Inform TU
     *             +--|           |-------+
     *     300-699 |  +-----------+       |
     *     ACK sent|    ^  |              |
     *             |    |  |              |
     *             +----+  |              |
     *                     |Timer D fires |
     *                     |-             |
     *                     +--------+     |
     *                              V     V
     *                             +------------+
     *                             |            |
     *                             | Terminated |
     *                             |            |
     *                             +------------+
     * </pre>
     * @param event
     */
    private final void onCompleted(final Event event) {
        if (event.isSipResponseEvent()) {
            final SipResponse response = event.response();
            if (response.getStatus() >= 300) {
                ctx().forwardUpstream(event);
                ack();
            }

            // If this was a 2xx response then
            // this isn't supposed to happen. Don't have
            // a great way of handling it and it is against
            // the RFC so silently ignore it.

        }
    }

    private final void onEnterCompleted(final Event event) {
        timerD = scheduleTimer(SipTimer.D, timersConfig.getTimerD());
    }

    private final void onExitCompleted(final Event event) {
        if (timerD != null) {
            timerD.cancel();
        }
    }

    /**
     * For Error responses, we will generate the ACK as part of this
     * transaction.
     */
    private void ack() {
        // TODO: generate the ACK for the error response case
    }

    /**
     * Retransmit the invite again
     */
    private void retransmitInvite() {
        ctx().forwardDownstream(Event.create(originalInvite));
    }

    @Override
    public TransactionId id() {
        return id;
    }

    @Override
    public void stop() {

    }

    @Override
    public void postStop() {

    }

    private final Cancellable scheduleTimer(final SipTimer timer, final Duration duration) {
        return ctx().scheduler().schedule(timer, duration);
    }

    @Override
    protected Logger logger() {
        return logger;
    }

}
