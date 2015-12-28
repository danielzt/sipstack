package io.sipstack.application.impl;

import io.pkts.buffer.Buffer;
import io.pkts.buffer.Buffers;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.pkts.packet.sip.header.SipHeader;
import io.sipstack.application.B2BUA;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * @author ajansson@twilio.com
 */
public class DefaultB2BUA implements B2BUA {

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
        COPY_HEADERS.add(Buffers.wrap("X-Twilio-Request-URI"));
        COPY_HEADERS.add(Buffers.wrap("X-Twilio-SrcIp"));
        //COPY_HEADERS.add(Buffers.wrap("X-Twilio-AccountFlags"));
        COPY_HEADERS.add(Buffers.wrap("X-Twilio-VoiceTrace"));
        COPY_HEADERS.add(Buffers.wrap("X-Twilio-MediaGateway"));
        COPY_HEADERS.add(Buffers.wrap("X-Twilio-MediaFeatures"));
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
        throw new RuntimeException("TODO: re-writing a bunch of stuff so...");
        /*
        if (request.isInvite()) {
            final SipRequest.Builder builder = SipRequest.invite(target.getTarget())
                    .from(request.getFromHeader())
                    .to(request.getToHeader());

            COPY_HEADERS.forEach(name -> request.getHeader(name).ifPresent(builder::header));

            // Copy content
            builder.header(SipHeader.create("Content-Type", "application/sdp"));
            builder.header(request.getHeader("Content-Length").get());
            builder.content(request.getContent());

            requestHandlers.forEach(h -> h.accept(request, builder));

            final SipRequest requestB = builder.build();
            target.send(builder);
        } else if (request.isAck()) {
            final SipRequest.Builder builder = target.createAck();

            requestHandlers.forEach(h -> h.accept(request, builder));

            target.send(builder);
        } else if (request.isBye()) {
            // TODO ugly correlation
            byeRequest = request;

            final SipRequest.Builder builder = target.createBye();

            requestHandlers.forEach(h -> h.accept(request, builder));

            target.send(builder);

        } else {
            // throw new RuntimeException("TODO");
        }
        */
    }

    private void processResponse(final DefaultUA target, final SipResponse response) {
        final SipRequest linkedRequest = response.isInvite() ? target.getRequest() : byeRequest;
        final SipResponse builder = linkedRequest.createResponse(response.getStatus()).build();
        // TODO: set the content.

        if (response.hasContent()) {
            // Copy content
            builder.setHeader(SipHeader.create("Content-Type", "application/sdp"));
            builder.setHeader(response.getHeader("Content-Length").get());
        }

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
