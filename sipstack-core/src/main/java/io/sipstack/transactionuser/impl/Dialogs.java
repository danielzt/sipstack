package io.sipstack.transactionuser.impl;

import java.util.function.Consumer;

import io.pkts.buffer.Buffer;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.address.Address;
import io.pkts.packet.sip.address.SipURI;
import io.pkts.packet.sip.header.ToHeader;
import io.pkts.packet.sip.header.impl.AddressParametersHeaderImpl;
import io.sipstack.netty.codec.sip.Transport;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.Transactions;
import io.sipstack.transactionuser.Dialog;
import io.sipstack.transactionuser.TransactionUserEvent;
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

    private enum State {TRYING, PROCEEDING, EARLY, CONFIRMED, TERMINATED}

    private Transactions transactions;
    private final SipRequest request;
    // For now just one dialog
    private final MyDialog dialog;
    private Consumer<TransactionUserEvent> consumer;
    private Flow flow;

    public Dialogs(final Transactions transactions,
            final SipRequest request) {
        this.transactions = transactions;
        this.request = request;
        this.dialog = new MyDialog();
    }

    public String id() {
        // For now just use call-id as id
        return request.getCallIDHeader().getCallId().toString();
    }

    public Dialog process(final Transaction tx, final SipMessage message) {
        if (tx != null) {
            this.flow = tx.flow();
        }
        if (message.isResponse() && message.getToHeader().getTag() != null) {
            dialog.setToTag(message.getToHeader().getTag());
        }
        return dialog;
    }

    public class MyDialog implements Dialog {
        private State state = State.TRYING;
        private Buffer toTag;

        public MyDialog() {
        }

        @Override
        public String id() {
            return Dialogs.this.id();
        }

        @Override
        public Consumer<TransactionUserEvent> getConsumer() {
            return Dialogs.this.consumer;
        }

        public void setConsumer(final Consumer<TransactionUserEvent> consumer) {
            Dialogs.this.consumer = consumer;
        }

        @Override
        public void send(final SipMessage message) {
            if (flow == null) {
                final SipURI target = (SipURI) message.toRequest().getRequestUri();
                transactions.createFlow(target.getHost())
                        .withPort(target.getPort())
                        .withTransport(Transport.udp)
                        .onSuccess(f -> {
                            flow = f;
                            transactions.send(f, message);
                        })
                        .connect();

            } else {
                transactions.send(flow, message);
            }
        }

        @Override
        public SipRequest.Builder createAck() {
            final ToHeader to = request.getToHeader().clone();
            if (toTag != null) {
                // TODO ugly internal class
                to.setParameter(AddressParametersHeaderImpl.TAG, toTag);
            }
            return SipRequest.ack((SipURI) request.getRequestUri())
                    .from(request.getFromHeader())
                    .to(to)
                    .callId(request.getCallIDHeader());
        }

        public void setToTag(final Buffer toTag) {
            this.toTag = toTag;
        }
    }

}
