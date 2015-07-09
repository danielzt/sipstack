package io.sipstack.application.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import io.pkts.buffer.Buffer;
import io.pkts.buffer.Buffers;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.pkts.packet.sip.header.ViaHeader;
import io.sipstack.application.B2BUA;

/**
 * @author ajansson@twilio.com
 */
public class DefaultB2BUA implements B2BUA {

    private static final String LOCAL_HOST = System.getProperty("localhost", "127.0.0.1");

    private static final Set<Buffer> COPY_HEADERS = new HashSet<>();
    static {
        COPY_HEADERS.add(Buffers.wrap("X-Twilio-Orig-Call-ID"));
        COPY_HEADERS.add(Buffers.wrap("X-Twilio-Media-Features"));
        COPY_HEADERS.add(Buffers.wrap("X-Twilio-Zone"));
        COPY_HEADERS.add(Buffers.wrap("X-Twilio-AccountSid"));
        COPY_HEADERS.add(Buffers.wrap("X-Twilio-ProviderSid"));
        COPY_HEADERS.add(Buffers.wrap("X-Twilio-PhoneNumberSid"));
        COPY_HEADERS.add(Buffers.wrap("X-Twilio-ApiVersion"));
        COPY_HEADERS.add(Buffers.wrap("X-Twilio-OutboundMediaSecurity"));
        COPY_HEADERS.add(Buffers.wrap("X-Twilio-TrunkSid"));
        COPY_HEADERS.add(Buffers.wrap("X-Twilio-Original-Request-URI"));
        COPY_HEADERS.add(Buffers.wrap("X-Twilio-CallSid"));
    }

    private final String friendlyName;
    private final DefaultUA uaA;
    private final DefaultUA uaB;
    private final List<RequestHandler> requestHandlers = new ArrayList<>();
    private final List<ResponseHandler> responseHandlers = new ArrayList<>();
    private SipRequest byeRequest;

    public DefaultB2BUA(final String friendlyName, final DefaultUA uaA, final DefaultUA uaB) {
        this.friendlyName = friendlyName;
        this.uaA = uaA;
        this.uaB = uaB;

        uaA.addHandler(m -> processMessage(uaB, m));
        uaB.addHandler(m -> processMessage(uaA, m));
    }

    public String friendlyName() {
        return friendlyName;
    }

    @Override
    public void start() {
        processRequest(uaB, uaA.getRequest());
    }

    @Override
    public RequestStream onRequest() {
        return new MyRequestStream();
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

    private void processMessage(final DefaultUA target, final SipMessage message) {
        if (message.isRequest()) {
            processRequest(target, message.toRequest());
        } else {
            processResponse(target, message.toResponse());
        }
    }

    private void processRequest(final DefaultUA target, final SipRequest request) {
        if (request.isInvite()) {
            final SipRequest.Builder builder = SipRequest.invite(target.getTarget())
                    .from(request.getFromHeader())
                    .to(request.getToHeader())
                    .contact(request.getContactHeader());

            COPY_HEADERS.forEach(name -> request.getHeader(name).ifPresent(builder::header));

            requestHandlers.forEach(h -> h.accept(request, builder));

            final SipRequest requestB = builder.build();
            final ViaHeader via = ViaHeader.with()
                    .host(LOCAL_HOST)
                    .port(5060)
                    .transportUDP()
                    .branch(ViaHeader.generateBranch())
                    .build();
            requestB.addHeaderFirst(via);
            target.send(requestB);
        } else if (request.isAck()) {
            final SipRequest.Builder builder = target.createAck();

            requestHandlers.forEach(h -> h.accept(request, builder));

            final SipRequest requestB = builder.build();
            final ViaHeader via = ViaHeader.with()
                    .host(LOCAL_HOST)
                    .port(5060)
                    .transportUDP()
                    .branch(ViaHeader.generateBranch())
                    .build();
            requestB.addHeaderFirst(via);
            target.send(requestB);
        } else if (request.isBye()) {
            // TODO ugly correlation
            byeRequest = request;

            final SipRequest.Builder builder = target.createBye();

            requestHandlers.forEach(h -> h.accept(request, builder));

            final SipRequest requestB = builder.build();
            final ViaHeader via = ViaHeader.with()
                    .host(LOCAL_HOST)
                    .port(5060)
                    .transportUDP()
                    .branch(ViaHeader.generateBranch())
                    .build();
            requestB.addHeaderFirst(via);
            target.send(requestB);

        } else {
            throw new RuntimeException("TODO");
        }
    }

    private void processResponse(final DefaultUA target, final SipResponse response) {
        final SipRequest linkedRequest = response.isInvite() ? target.getRequest() : byeRequest;
        final SipResponse builder = linkedRequest.createResponse(response.getStatus());

        responseHandlers.forEach(h -> h.accept(response, builder));

        target.send(builder);
    }

    class MyRequestStream implements RequestStream {

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
