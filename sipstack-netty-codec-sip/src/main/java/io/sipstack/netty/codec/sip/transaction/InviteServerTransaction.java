package io.sipstack.netty.codec.sip.transaction;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.netty.codec.sip.actor.ActorSupport;
import io.sipstack.netty.codec.sip.actor.Cancellable;
import io.sipstack.netty.codec.sip.config.TimersConfiguration;
import io.sipstack.netty.codec.sip.config.TransactionLayerConfiguration;
import io.sipstack.netty.codec.sip.event.Event;
import io.sipstack.netty.codec.sip.event.SipMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Implements the Invite Server Transaction state machine as specified by rfc6026.
 *
 * <pre>
 *
 *                                    |INVITE
 *                                    |pass INV to TU
 *                 INVITE             V send 100 if TU won't in 200 ms
 *                 send response+------------+
 *                     +--------|            |--------+ 101-199 from TU
 *                     |        |            |        | send response
 *                     +------->|            |<-------+
 *                              | Proceeding |
 *                              |            |--------+ Transport Err.
 *                              |            |        | Inform TU
 *                              |            |<-------+
 *                              +------------+
 *                 300-699 from TU |    |2xx from TU
 *                 send response   |    |send response
 *                  +--------------+    +------------+
 *                  |                                |
 * INVITE           V          Timer G fires         |
 * send response +-----------+ send response         |
 *      +--------|           |--------+              |
 *      |        |           |        |              |
 *      +------->| Completed |<-------+      INVITE  |  Transport Err.
 *               |           |               -       |  Inform TU
 *      +--------|           |----+          +-----+ |  +---+
 *      |        +-----------+    | ACK      |     | v  |   v
 *      |          ^   |          | -        |  +------------+
 *      |          |   |          |          |  |            |---+ ACK
 *      +----------+   |          |          +->|  Accepted  |   | to TU
 *      Transport Err. |          |             |            |<--+
 *      Inform TU      |          V             +------------+
 *                     |      +-----------+        |  ^     |
 *                     |      |           |        |  |     |
 *                     |      | Confirmed |        |  +-----+
 *                     |      |           |        |  2xx from TU
 *       Timer H fires |      +-----------+        |  send response
 *       -             |          |                |
 *                     |          | Timer I fires  |
 *                     |          | -              | Timer L fires
 *                     |          V                | -
 *                     |        +------------+     |
 *                     |        |            |<----+
 *                     +------->| Terminated |
 *                              |            |
 *                              +------------+
 *
 * </pre>
 *
 * @author jonas@jonasborjesson.com
 */
public class InviteServerTransaction extends ActorSupport<Event, TransactionState> implements Transaction {

    private static final Logger logger = LoggerFactory.getLogger(InviteServerTransaction.class);

    private final TransactionId id;

    private final SipRequest originalInvite;

    private final TransactionLayerConfiguration config;

    private final TimersConfiguration timersConfig;

    private SipResponse lastResponse;

    private Cancellable timer100Trying;

    /**
     * If we enter the completed state we will setup Timer G for response retransmissions if this
     * transaction is over an unreliable transport.
     */
    private Cancellable timerG;


    /**
     * How many times Timer G has fired.
     */
    private int timerGCount;

    /**
     * If we enter the completed state, Timer H will take us from completed to terminated unless we
     * do receive an ACK before then. Hence, Timer H determines the longest amount of time we can
     * stay in the completed state.
     */
    private Cancellable timerH;

    /**
     * If we enter the accepted state, Timer L will let us know when it is time to transition
     * over to the terminated state. The accepted state is a "new" state introduced by RFC6026
     * in order to deal with re-transmitted INVITEs. In the old state machine, we transitioned
     * directly to terminated state as soon as we got a 2xx to the INVITE which meant that
     * any re-transmissions on the INVITE created a brand new transaction, which is not what
     * we want.
     */
    private Cancellable timerL;


    /**
     * @param id
     * @param initialState
     * @param values
     */
    protected InviteServerTransaction(final TransactionId id,
                                      final SipRequest invite,
                                      final TransactionLayerConfiguration config) {
        super(id.toString(), TransactionState.INIT, TransactionState.values());
        this.id = id;
        this.originalInvite = invite;
        this.config = config;
        this.timersConfig = config.getTimers();

        when(TransactionState.INIT, init);

        // when(TransactionState.PROCEEDING, proceeding);
        // onEnter(TransactionState.PROCEEDING, onEnterProceeding);
        // onExit(TransactionState.PROCEEDING, onExitProceeding);
    }

    /**
     * The init state. Just make sure that the first event we receive is the same INVITE as created
     * the transaction (yes, we compare references in this case, that's what we want) and then
     * transition over to the proceeding state.
     */
    private final Consumer<Event> init = event -> {
        if (event.isSipMessageEvent() && event.toSipMessageEvent().message() == originalInvite()) {
            ctx().forwardUpstream(event);
        } else {
            System.err.println("Queue??? shouldn't be able to happen. " + event);
        }
        become(TransactionState.PROCEEDING);
    };

    /**
     * Implements the proceeding state as follows:
     *
     * <pre>
     *
     *                                    |INVITE
     *                                    |pass INV to TU
     *                 INVITE             V send 100 if TU won't in 200 ms
     *                 send response+------------+
     *                     +--------|            |--------+ 101-199 from TU
     *                     |        |            |        | send response
     *                     +------->|            |<-------+
     *                              | Proceeding |
     *                              |            |--------+ Transport Err.
     *                              |            |        | Inform TU
     *                              |            |<-------+
     *                              +------------+
     *                 300-699 from TU |    |2xx from TU
     *                 send response   |    |send response
     *                  +--------------+    +------------+
     *                  |                                |
     *                  V                                V
     *         +------------+                     +------------+
     *         |  Completed |                     |  Accepted  |
     *         +------------+                     +------------+
     * </pre>
     */
    private final Consumer<Event> proceeding = event -> {
        if (event.isSipMessageEvent()) {
            final SipMessageEvent sipMsgEvent = event.toSipMessageEvent();
            final SipMessage msg = sipMsgEvent.message();

            if (isRetransmittedInvite(msg)) {
                if (lastResponse != null) {
                    relayResponse(event.toSipMessageEvent());
                }
                return;
            }

            final SipResponse response = msg.toResponse();
            relayResponse(sipMsgEvent);
            if (response.isSuccess()) {
                become(TransactionState.ACCEPTED);
                // become(TransactionState.TERMINATED);
            } else if (response.isFinal()) {
                become(TransactionState.COMPLETED);
            }
        } else if (event.isSipTimer100Trying()) {
            final SipResponse response = originalInvite().createResponse(100);
            ctx().forwardDownstream(null);
        } else {
            unhandled(event);
        }
    };

    /**
     * When entering the Proceeding state we may send a 100 Trying right away (depending on configuration) or we may
     * delay it with 200 ms and send it later unless TU already have sent some kind of response (any kind really)
     */
    private final Consumer<Event> onEnterProceeding = event -> {
        if (config().isSend100TryingImmediately()) {
            final SipMessage invite = originalInvite();
            if (invite.isAck()) {
                System.err.println("What the fuck, this is an ACK!");
            }
            final SipResponse response = invite.createResponse(100);
            // downstream.tell(IOWriteEvent.create(response), self());
        } else {
            // TODO: schedule timer...
            // timer100Trying = ctx().scheduler().schedule();
        }
    };

    private final Consumer<Event> onExitProceeding = event -> {
        if (timer100Trying != null) {
            timer100Trying.cancel();
        }
    };

    private void relayResponse(final SipMessageEvent event) {
        if (lastResponse == null
                || lastResponse.getStatus() < event.message().toResponse().getStatus()) {
            lastResponse = event.message().toResponse();
        }
        ctx().forwardDownstream(event);
    }

    /**
     * Check whether the request is a re-transmitted INVITE.
     * @param msg
     * @return
     */
    private boolean isRetransmittedInvite(SipMessage msg) {
        if (msg.isRequest() && msg.isInvite()) {
            // TODO: actually check it...
            return true;
        }
        return false;
    }

    private TransactionLayerConfiguration config() {
        return config;
    }

    private SipRequest originalInvite() {
        return originalInvite;
    }

    @Override
    protected final Logger logger() {
        return logger;
    }
}
