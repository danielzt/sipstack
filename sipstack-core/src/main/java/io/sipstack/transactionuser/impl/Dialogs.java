package io.sipstack.transactionuser.impl;

import java.util.function.Consumer;

import io.pkts.buffer.Buffer;
import io.pkts.buffer.Buffers;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.header.ToHeader;
import io.pkts.packet.sip.header.impl.AddressParametersHeaderImpl;
import io.sipstack.netty.codec.sip.Transport;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.Transactions;
import io.sipstack.transactionuser.Dialog;
import io.sipstack.transactionuser.DialogEvent;
import io.sipstack.transactionuser.TransactionEvent;
import io.sipstack.transport.Flow;

/**
 * Implements https://tools.ietf.org/html/rfc4235#section-3.7.1
 *
 *     +----------+            +----------+
 *     |          | 1xx-notag  |          |
 *     |          |----------->|          |
 *     |  Trying  |            |Proceeding|-----+
 *     |          |---+  +-----|          |     |
 *     |          |   |  |     |          |     |
 *     +----------+   |  |     +----------+     |
 *          |   |     |  |          |           |
 *          |   |     |  |          |           |
 *          +<--C-----C--+          |1xx-tag    |
 *          |   |     |             |           |
 * cancelled|   |     |             V           |
 *  rejected|   |     |1xx-tag +----------+     |
 *          |   |     +------->|          |     |2xx
 *          |   |              |          |     |
 *          +<--C--------------|  Early   |-----C---+ 1xx-tag
 *          |   |   replaced   |          |     |   | w/new tag
 *          |   |              |          |<----C---+ (new FSM
 *          |   |              +----------+     |      instance
 *          |   |   2xx             |           |      created)
 *          |   +----------------+  |           |
 *          |                    |  |2xx        |
 *          |                    |  |           |
 *          V                    V  V           |
 *     +----------+            +----------+     |
 *     |          |            |          |     |
 *     |          |            |          |     |
 *     |Terminated|<-----------| Confirmed|<----+
 *     |          |  error     |          |
 *     |          |  timeout   |          |
 *     +----------+  replaced  +----------+
 *                   local-bye   |      ^
 *                   remote-bye  |      |
 *                               |      |
 *                               +------+
 *                                2xx w. new tag
 *                                 (new FSM instance
 *                                  created)
 * @author ajansson@twilio.com
 */

public class Dialogs {

    private static final String REMOTE_HOST = System.getProperty("remotehost", "127.0.0.1");
    private static final int REMOTE_PORT = Integer.getInteger("remoteport", 5060);

    private enum State {TRYING, PROCEEDING, EARLY, CONFIRMED, TERMINATED}

    private final Consumer<DialogEvent> upstream;
    private final Transactions transactions;
    private final SipRequest request;
    private final Buffer callId;
    private final Buffer localTag;
    private Flow flow;

    // For now support only one dialog
    private final MyDialog dialog = new MyDialog();

    public Dialogs(final Consumer<DialogEvent> upstream, final Transactions transactions,
            final Transaction tx, final SipRequest request, final boolean isUpstream) {
        this.upstream = upstream;
        this.transactions = transactions;
        this.request = request;
        this.callId = request.getCallIDHeader().getCallId();
        this.localTag = getLocalTag(request, isUpstream);
        if (tx != null) {
            this.flow = tx.flow();
        }
    }

    /**
     * Gets the dialog key (call id + local tag)
     * @param message SIP message
     * @return Dialog key.
     */
    public static Buffer getDialogKey(final SipMessage message, final boolean isUpstream) {
        return Buffers.wrap(message.getCallIDHeader().getCallId(), getLocalTag(message, isUpstream));
    }

    public void dispatchUpstream(final TransactionEvent event) {
        dialog.dispatchUpstream(event);
    }

    public Dialog getDialog(final SipMessage message) {
        return dialog;
    }

    private static Buffer getLocalTag(final SipMessage message, final boolean isUpstream) {
        if (isUpstream) {
            if (message.isRequest()) {
                return message.getToHeader().getTag();
            } else {
                return message.getFromHeader().getTag();
            }
        } else {
            if (message.isRequest()) {
                return message.getFromHeader().getTag();
            } else {
                return message.getToHeader().getTag();
            }
        }
    }

    private static Buffer getRemoteTag(final SipMessage message, final boolean isUpstream) {
        if (isUpstream) {
            if (message.isRequest()) {
                return message.getFromHeader().getTag();
            } else {
                return message.getToHeader().getTag();
            }
        } else {
            if (message.isRequest()) {
                return message.getToHeader().getTag();
            } else {
                return message.getFromHeader().getTag();
            }
        }
    }

    public class MyDialog implements Dialog {
        private State state = State.TRYING;
        private Buffer remoteTag;
        private Buffer id;

        public MyDialog() {
        }

        @Override
        public void send(final SipMessage message) {
            if (flow == null) {
                transactions.createFlow(REMOTE_HOST)
                        .withPort(REMOTE_PORT)
                        .withTransport(Transport.udp)
                        .onSuccess(f -> {
                            flow = f;
                            final Transaction t = transactions.send(f, message);
                        })
                        .connect();

            } else {
                final Transaction t = transactions.send(flow, message);
            }
        }

        @Override
        public SipRequest.Builder createAck() {
            final ToHeader to = request.getToHeader().clone();
            if (remoteTag != null) {
                // TODO ugly internal class
                to.setParameter(AddressParametersHeaderImpl.TAG, remoteTag);
            }
            return SipRequest.ack(request.getRequestUri())
                    .from(request.getFromHeader())
                    .to(to)
                    .callId(request.getCallIDHeader());
        }

        @Override
        public SipRequest.Builder createBye() {
            final ToHeader to = request.getToHeader().clone();
            if (remoteTag != null) {
                // TODO ugly internal class
                to.setParameter(AddressParametersHeaderImpl.TAG, remoteTag);
            }
            return SipRequest.bye(request.getRequestUri())
                    .from(request.getFromHeader())
                    .to(to)
                    .callId(request.getCallIDHeader());
        }

        public void dispatchUpstream(final TransactionEvent event) {
            final Buffer remoteTag = getRemoteTag(event.message(), true);
            if (event.message().isResponse() && remoteTag != null) {
                this.remoteTag = remoteTag;
            }
            upstream.accept(new DefaultDialogEvent(this, event));
        }
    }

}
