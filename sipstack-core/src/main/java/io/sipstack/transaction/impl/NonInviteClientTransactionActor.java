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
 * Implements the following state machine (rfc3261 section 17.1.2)
 *
 * <pre>
 *
 *                       |Request from TU
 *                       |send request
 *   Timer E             V
 *   send request  +-----------+
 *       +---------|           |-------------------+
 *       |         |  Trying   |  Timer F          |
 *       +-------->|           |  or Transport Err.|
 *                 +-----------+  inform TU        |
 *    200-699         |  |                         |
 *    resp. to TU     |  |1xx                      |
 *    +---------------+  |resp. to TU              |
 *    |                  |                         |
 *    |   Timer E        V       Timer F           |
 *    |   send req +-----------+ or Transport Err. |
 *    |  +---------|           | inform TU         |
 *    |  |         |Proceeding |------------------>|
 *    |  +-------->|           |-----+             |
 *    |            +-----------+     |1xx          |
 *    |              |      ^        |resp to TU   |
 *    | 200-699      |      +--------+             |
 *    | resp. to TU  |                             |
 *    |              |                             |
 *    |              V                             |
 *    |            +-----------+                   |
 *    |            |           |                   |
 *    |            | Completed |                   |
 *    |            |           |                   |
 *    |            +-----------+                   |
 *    |              ^   |                         |
 *    |              |   | Timer K                 |
 *    +--------------+   | -                       |
 *                       |                         |
 *                       V                         |
 *                 +-----------+                   |
 *                 |           |                   |
 *                 | Terminated|<------------------+
 *                 |           |
 *                 +-----------+
 *
 * </pre>
 * @author jonas@jonasborjesson.com
 */
public class NonInviteClientTransactionActor extends ActorSupport<Event, TransactionState> implements TransactionActor {

    private static final Logger logger = LoggerFactory.getLogger(NonInviteClientTransactionActor.class);

    private final TransactionId id;

    private final SipRequest originalRequest;

    private SipResponse lastResponse;

    private final TransactionLayerConfiguration config;

    private final TimersConfiguration timersConfig;

    /**
     * If we enter the trying state, Timer F will let us know when it is time to transition
     * over to the terminated state for the case when we get absolutely nothing back.
     */
    private Cancellable timerF;


    /**
     * Timer E is the re-transmit timer while in the Trying and Proceeding state.
     */
    private Cancellable timerE;

    /**
     * How many times Timer E has fired.
     */
    private int timerECount;

    /**
     * Timer K is the timer for getting out of the completed state to terminated
     * and is there to consume any re-transmissions. Note, this timer is ONLY
     * scheduled for unreliable transports.
     */
    private Cancellable timerK;

    /**
     * TODO: needs to be passed in.
     */
    private final boolean isUsingUnreliableTransport = true;

    protected NonInviteClientTransactionActor(final TransactionId id,
                                              final SipRequest request,
                                              final TransactionLayerConfiguration config) {
        super(id.toString(), TransactionState.INIT, TransactionState.TERMINATED, TransactionState.values());
        this.id = id;
        this.originalRequest = request;
        this.config = config;
        this.timersConfig = config.getTimers();

        when(TransactionState.INIT, this::onInit);

        when(TransactionState.TRYING, this::onTrying);
        onEnter(TransactionState.TRYING, this::onEnterTrying);
        onExit(TransactionState.TRYING, this::onExitTrying);

        when(TransactionState.PROCEEDING, this::onProceeding);
        onEnter(TransactionState.PROCEEDING, this::onEnterProceeding);
        onExit(TransactionState.PROCEEDING, this::onExitProceeding);

        // Note: not onExit needed for completed because
        // we have nothing to do.
        when(TransactionState.COMPLETED, this::onCompleted);

        // only need onEnterCompleted if unreliable because otherwise
        // we won't actually schedule Timer K and there is nothing else
        // to do so...
        if (isUsingUnreliableTransport) {
            onEnter(TransactionState.COMPLETED, this::onEnterCompleted);
        }
    }

    public boolean isClientTransaction() {
        return true;
    }

    private void onInit(final Event event) {
        if (event.isSipRequestEvent() && event.request() == originalRequest) {
            ctx().forwardDownstream(event);
        } else {
            System.err.println("Queue??? shouldn't be able to happen. " + event);
        }

        become(TransactionState.TRYING);
    };

    private void onTrying(final Event event) {
        if (event.isSipResponseEvent()) {
            ctx().forwardUpstream(event);
            final SipResponse response = event.response();
            if (response.isProvisional()) {
                become(TransactionState.PROCEEDING);
            } else if (response.isFinal()) {
                become(TransactionState.COMPLETED);
            }
        } else if (event.isSipTimerE()) {
            ++timerECount;
            timerE = scheduleTimer(SipTimer.E, calculateNextTimerE());
            retransmitOriginalRequest();
        } else if (event.isSipTimerF()) {
            become(TransactionState.TERMINATED);
        }
    };

    private void onEnterTrying(final Event event) {
        timerE = scheduleTimer(SipTimer.E, calculateNextTimerE());
        timerF = scheduleTimer(SipTimer.F, timersConfig.getTimerF());
    };

    private void onExitTrying(final Event event) {
        timerE.cancel();
        timerF.cancel();
    };

    private void onProceeding(final Event event) {
        if (event.isSipResponseEvent()) {
            ctx().forwardUpstream(event);
            final SipResponse response = event.response();
            if (response.isFinal()) {
                become(TransactionState.COMPLETED);
            }
        } else if (event.isSipTimerE()) {
            ++timerECount;
            timerE = scheduleTimer(SipTimer.E, calculateNextTimerE());
            retransmitOriginalRequest();
        } else if (event.isSipTimerF()) {
            become(TransactionState.TERMINATED);
        }
    }

    private void onEnterProceeding(final Event event) {
        timerECount = 0;
        timerE = scheduleTimer(SipTimer.E, calculateNextTimerE());
        timerF = scheduleTimer(SipTimer.F, timersConfig.getTimerF());
    }

    private void onExitProceeding(final Event event) {
        timerE.cancel();
        timerF.cancel();
    }

    /**
     * Nothing really to do in the completed state. Any responses that
     * gets re-transmitted should just be consumed and the easiest way to
     * do that is to do nothing.
     *
     * The only event we care about is Timer K (for unreliable transports), which
     * will simply move us over to TERMINATED.
     *
     * If we are using a reliable transport, then we simply move over to
     * TERMINATED right away and that's that.
     *
     * @param event
     */
    private void onCompleted(final Event event) {
        if (!isUsingUnreliableTransport) {
            become(TransactionState.TERMINATED);
        }

        if (event.isSipTimerK()) {
            become(TransactionState.TERMINATED);
        }
    }

    private void onEnterCompleted(final Event event) {
        timerK = scheduleTimer(SipTimer.K, timersConfig.getTimerK());
    }

    /**
     * While in the completed state we will re-transmit the final response (which then must be a
     * error response only otherwise we wouldn't be in the completed state to begin with) every time
     * this timer fires.
     *
     * @return
     */
    private Duration calculateNextTimerE(){
        final long defaultTimerE = timersConfig.getTimerE().toMillis();
        final long t2 = timersConfig.getT2().toMillis();
        return Utils.calculateBackoffTimer(timerECount, defaultTimerE, t2);
    }

    private final Cancellable scheduleTimer(final SipTimer timer, final Duration duration) {
        return ctx().scheduler().schedule(timer, duration);
    }

    /**
     * Retransmit the original request again
     */
    private void retransmitOriginalRequest() {
        ctx().forwardDownstream(Event.create(originalRequest));
    }

    @Override
    public TransactionId id() {
        return id;
    }

    @Override
    protected Logger logger() {
        return logger;
    }
}
