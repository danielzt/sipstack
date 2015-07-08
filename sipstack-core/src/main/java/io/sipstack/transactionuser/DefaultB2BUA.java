package io.sipstack.transactionuser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.pkts.packet.sip.header.ViaHeader;

/**
 * @author ajansson@twilio.com
 */
public class DefaultB2BUA implements B2BUA {

    private final String friendlyName;
    private final DefaultUA uaA;
    private final DefaultUA uaB;
    private final List<RequestHandler> requestHandlers = new ArrayList<>();
    private final List<ResponseHandler> responseHandlers = new ArrayList<>();

    public DefaultB2BUA(final String friendlyName, final DefaultUA uaA, final DefaultUA uaB) {
        this.friendlyName = friendlyName;
        this.uaA = uaA;
        this.uaB = uaB;

        uaA.addHandler(m -> processMessage(uaA, uaB, m));
        uaB.addHandler(m -> processMessage(uaB, uaA, m));
    }

    public String friendlyName() {
        return friendlyName;
    }

    @Override
    public void start() {
        processRequest(uaA, uaB, uaA.getRequest());
    }

    @Override
    public RequestStream onRequest() {
        return new MyReqeuestStream();
    }

    @Override
    public ResponseStream onResponse() {
        return new MyResponseStream();
    }

    @Override
    public DefaultUA getUaA() {
        return uaA;
    }

    @Override
    public DefaultUA getUaB() {
        return uaB;
    }

    private void processMessage(final DefaultUA source, final DefaultUA target, final SipMessage message) {
        if (message.isRequest()) {
            processRequest(source, target, message.toRequest());
        } else {
            processResponse(source, target, message.toResponse());
        }
    }

    private void processRequest(final DefaultUA source, final DefaultUA target, final SipRequest request) {
        final SipRequest.Builder builder = SipRequest.request(request.getMethod(), target.getTarget());
        builder.from(request.getFromHeader());
        builder.to(request.getToHeader());
        builder.contact(request.getContactHeader());

        requestHandlers.forEach(h -> h.accept(request, builder));

        final SipRequest requestB = builder.build();
        final ViaHeader via =
                ViaHeader.with().host("127.0.0.1").port(5060).transportUDP().branch(ViaHeader.generateBranch()).build();
        requestB.addHeaderFirst(via);
        target.send(requestB);
    }

    private void processResponse(final DefaultUA source, final DefaultUA target, final SipResponse response) {
        final SipResponse builder = null; // TODO

        responseHandlers.forEach(h -> h.accept(response, builder));

        uaB.send(builder);
    }

    class MyReqeuestStream implements RequestStream {

        private Predicate<SipRequest> filter;

        @Override
        public void doProcess(final RequestProcessor processor) {
            requestHandlers.add(new RequestHandler(this.filter, processor));
        }

        @Override
        public RequestStream filter(final Predicate<SipRequest> filter) {
            this.filter = filter;
            return this;
        }
    }

    class RequestHandler implements BiConsumer<SipRequest, SipRequest.Builder> {
        private final Predicate<SipRequest> filter;
        private final RequestProcessor processor;

        public RequestHandler(final Predicate<SipRequest> filter, final RequestProcessor processor) {
            this.filter = filter;
            this.processor = processor;
        }

        @Override
        public void accept(final SipRequest request, final SipRequest.Builder builder) {
            if (filter.test(request)) {
                processor.process(DefaultB2BUA.this, request, builder);
            }
        }
    }

    class MyResponseStream implements ResponseStream {

        private Predicate<SipResponse> filter;

        @Override
        public void doProcess(final ResponseProcessor processor) {
            responseHandlers.add(new ResponseHandler(this.filter, processor));
        }

        @Override
        public ResponseStream filter(final Predicate<SipResponse> filter) {
            this.filter = filter;
            return this;
        }
    }

    class ResponseHandler implements BiConsumer<SipResponse, SipResponse> {
        private final Predicate<SipResponse> filter;
        private final ResponseProcessor processor;

        public ResponseHandler(final Predicate<SipResponse> filter, final ResponseProcessor processor) {
            this.filter = filter;
            this.processor = processor;
        }

        @Override
        public void accept(final SipResponse response, final SipResponse builder) {
            if (filter.test(response)) {
                processor.process(DefaultB2BUA.this, response, builder);
            }
        }
    }
}
