package io.sipstack.transaction.impl;

import io.hektor.core.ActorRef;
import io.hektor.core.Cancellable;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.actor.ActorSupport;
import io.sipstack.config.TransactionLayerConfiguration;
import io.sipstack.event.Event;
import io.sipstack.event.IOReadEvent;
import io.sipstack.event.IOWriteEvent;
import io.sipstack.timers.SipTimer;
import io.sipstack.transaction.TransactionId;
import io.sipstack.transaction.TransactionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * Actually, need to implement the updated state machine as specified by rfc6026
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
 */
public class InviteServerTransactionActor extends ActorSupport<Event, TransactionState> {

    private static final Logger logger = LoggerFactory.getLogger(InviteServerTransactionActor.class);

    /**
     *
     */
    private final ActorRef upstream;

    /**
     * This will typically be a FlowActor but if that actor fails
     * we may have to reset it to a TransportSupervisor.
     */
    private ActorRef downstream;

    private final TransactionId id;
    private final IOReadEvent<SipMessage> originalInvite;

    private final TransactionLayerConfiguration config;

    private IOWriteEvent<SipMessage> lastResponse;

    private Cancellable timer100Trying;

    public InviteServerTransactionActor(final ActorRef upstream,
                                        final TransactionId id,
                                        final IOReadEvent<SipMessage> invite,
                                        final TransactionLayerConfiguration config) {
        super(id.toString(), TransactionState.INIT, TransactionState.values());
        this.upstream = upstream;
        this.id = id;
        this.originalInvite = invite;
        this.config = config;

        when(TransactionState.INIT, init);

        when(TransactionState.PROCEEDING, proceeding);
        onEnter(TransactionState.PROCEEDING, onEnterProceeding);
        onExit(TransactionState.PROCEEDING, onExitProceeding);
    }

    /**
     * The init state. Just make sure that the first event we receive is the same INVITE as created
     * the transaction (yes, we compare references in this case, that's what we want) and then
     * transition over to the proceeding state.
     */
    private final Consumer<Event> init = event -> {
        if (event == getInitialInviteEvent()) {
            downstream = sender();
            upstream().tell(event, self());
        } else {
            System.err.println("Queue??? shouldn't be able to happen");
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
        if (event.isSipIOEvent()) {
            final SipMessage msg = event.toSipIOEvent().getObject();
            if (isRetransmittedInvite(msg) && lastResponse != null) {
                relayResponse(lastResponse);
                return;
            }

            final SipResponse response = msg.toResponse();
            relayResponse(event.toSipIOWriteEvent());
            if (response.isSuccess()) {
                become(TransactionState.ACCEPTED);
                // become(TransactionState.TERMINATED);
            } else if (response.isFinal()) {
                become(TransactionState.COMPLETED);
            }
        } else if (event.isSipTimer100Trying()) {
            // Note, we have to reverse the context because when we
            // scheduled the 100 Trying event we had an 'upstream' context
            // since we were processing an incoming INVITE at that time.

            final SipResponse response = getInitialInviteEvent().getObject().createResponse(100);
            downstream.tell(IOWriteEvent.create(response), self());
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
            System.err.println("Sending 100 trying right away");
            final SipMessage invite = getInitialInviteEvent().getObject();
            if (invite.isAck()) {
                System.err.println("What the fuck, this is an ACK!");
            }
            final SipResponse response = invite.createResponse(100);
            downstream.tell(IOWriteEvent.create(response), self());
        } else {
            timer100Trying = ctx().scheduler().schedule(SipTimer.Trying, self(), self(), Duration.ofMillis(200));
        }
    };

    private TransactionLayerConfiguration config() {
        return config;
    }

    private final Consumer<Event> onExitProceeding = event -> {
        if (timer100Trying != null) {
            timer100Trying.cancel();
        }
    };

    private void relayResponse(final IOWriteEvent<SipMessage> response) {
        if (lastResponse == null
                || lastResponse.getObject().toResponse().getStatus() < response.getObject().toResponse().getStatus()) {
            lastResponse = response;
        }
        downstream.tell(response, sender());
    }

    private ActorRef upstream() {
        return upstream;
    }

    private IOReadEvent<SipMessage> getInitialInviteEvent() {
        return originalInvite;
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

    @Override
    protected final Logger logger() {
        return logger;
    }

}

