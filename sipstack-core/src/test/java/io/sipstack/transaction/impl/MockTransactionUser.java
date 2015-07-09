package io.sipstack.transaction.impl;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.pkts.packet.sip.address.SipURI;
import io.pkts.packet.sip.header.CSeqHeader;
import io.pkts.packet.sip.header.SipHeader;
import io.pkts.packet.sip.header.ViaHeader;
import io.sipstack.netty.codec.sip.Transport;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.TransactionId;
import io.sipstack.transaction.TransactionUser;
import io.sipstack.transaction.Transactions;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;

/**
 * @author jonas@jonasborjesson.com
 */
public class MockTransactionUser implements TransactionUser {

    /**
     * The {@link Transactions} API is our way into the transaction layer
     * and is how we will be writing messages back.
     */
    private Transactions transactionLayer;

    private SipAndTransactionStorage storage = new SipAndTransactionStorage();

    public void ensureTransactionTerminated(final TransactionId id) {
        storage.ensureTransactionTerminated(id);
    }

    public Transaction assertAndConsumeRequest(final String method) {
        return storage.assertAndConsumeRequest(method);
    }

    public Transaction assertAndConsumeResponse(final String method, final int responseStatus) {
        return storage.assertAndConsumeResponse(method, responseStatus);
    }

    /**
     * Poke this client to send out a new request through the transaction layer.
     *
     * @param request the request to send.
     * @return the transaction that got created for this request.
     */
    public void sendRequest(final SipRequest request) {
        transactionLayer.createFlow("127.0.0.1")
                .withTransport(Transport.udp)
                .withPort(5060)
                .onSuccess(f -> {
                    final Transaction t = transactionLayer.send(f, request);
                    storage.store(t, request);
                })
                .onFailure(f -> fail("Not sure why this failed"))
                .onCancelled(f -> fail("Who cancelled the flow future!"))
                .connect();
    }

    /**
     * Lookup a specific transaction based on the message
     *
     * @param msg
     * @return
     */
    public Transaction assertTransaction(final SipMessage msg) {
        final TransactionId id = TransactionId.create(msg);
        return storage.assertTransaction(id);
    }

    public Transaction assertTransaction(final TransactionId id) {
        return storage.assertTransaction(id);
    }

    public void reset() {
        storage.reset();
    }

    @Override
    public void start(final Transactions transactionLayer) {
        this.transactionLayer = transactionLayer;
    }

    @Override
    public void onRequest(final Transaction transaction, final SipRequest request) {
        storage.store(transaction, request);

        if (request.isAck()) {
            return;
        }

        final Optional<SipHeader> header = request.getHeader("X-Transaction-Test-Response");
        int responseCode = 200;
        if (header.isPresent()) {
            responseCode = Integer.valueOf(header.get().getValue().toString());
        }

        transactionLayer.send(transaction.flow(), request.createResponse(responseCode));

        /*
        FlowFuture flowFuture = Flow.withHost().withPort()...connect();

        transactionLayer.newFlow().withHost("127.0.0.1").withPort(5060).withTransport(Transport.tcp).onComplete(to something).onFailure(hello).connect();

        final Transaction anotherTransaction = transactionLayer.send(anotherFlow, request);
        */
    }

    @Override
    public void onResponse(final Transaction transaction, final SipResponse response) {
        storage.store(transaction, response);

        if (response.isSuccess() && response.isInvite()) {
            final ViaHeader via = ViaHeader.with()
                    .host("127.0.0.1")
                    .port(5099)
                    .transportUDP()
                    .branch(ViaHeader.generateBranch())
                    .build();

            // of course, the request-uri of the ack should be the contact etc but
            // we keep it simple here
            final CSeqHeader cSeq = CSeqHeader.with().cseq(response.getCSeqHeader().getSeqNumber()).method("ACK").build();
            final SipRequest ack = SipRequest.ack(SipURI.withHost("127.0.0.1").withPort(5090).build())
                    .callId(response.getCallIDHeader())
                    .from(response.getFromHeader())
                    .to(response.getToHeader())
                    .cseq(cSeq)
                    .via(via).build();

            final Transaction ackTransaction = transactionLayer.send(transaction.flow(), ack);
            assertThat(ackTransaction, not((Transaction)null));
            storage.store(ackTransaction, ack);
        }
    }

    @Override
    public void onTransactionTerminated(final Transaction transaction) {
        storage.onTransactionTerminated(transaction);
    }

    @Override
    public void onIOException(Transaction transaction, SipMessage msg) {
        assertThat(transaction, not((Transaction)null));
    }

}
