package io.sipstack.transactionuser.impl;

import java.util.function.Consumer;

import io.pkts.buffer.Buffer;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.address.SipURI;
import io.sipstack.netty.codec.sip.Transport;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.Transactions;
import io.sipstack.transactionuser.Dialog;
import io.sipstack.transactionuser.TransactionUserEvent;
import io.sipstack.transport.Flow;

/**
 * @author ajansson@twilio.com
 */
public class Dialogs {
    private Transactions transactions;
    // For now just use call-id as id
    private final Buffer id;
    // For now just one dialog
    private final Dialog dialog;
    private Flow flow;
    private Consumer<TransactionUserEvent> consumer;

    public Dialogs(final Transactions transactions,
            final SipMessage message, final Consumer<TransactionUserEvent> consumer) {
        this.transactions = transactions;
        this.id = message.getCallIDHeader().getCallId();
        this.consumer =  consumer;
        this.dialog = new MyDialog();
    }

    public String id() {
        return id.toString();
    }

    public Dialog getDialog(final SipMessage message) {
        return dialog;
    }

    public void setConsumer(final Consumer<TransactionUserEvent> consumer) {
        this.consumer = consumer;
    }

    public void receive(final Transaction transaction, final SipMessage message) {
        this.flow = transaction.flow();
        if (consumer != null) {
            consumer.accept(new TransactionUserEvent(dialog, transaction, message));
        }
    }

    public void send(final SipMessage message) {
    }

    public class MyDialog implements Dialog {
        public MyDialog() {
        }

        @Override
        public String id() {
            return Dialogs.this.id();
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
    }

}
