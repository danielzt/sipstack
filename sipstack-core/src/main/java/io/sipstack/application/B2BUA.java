package io.sipstack.application;

import java.util.function.Predicate;

import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;

/**
 * @author ajansson@twilio.com
 */
public interface B2BUA {

    void start();

    UA getUaA();

    UA getUaB();

    RequestStream onRequest();

    ResponseStream onResponse();

    interface Builder {

        B2BUA build();

        Builder withA(UA ua);

        Builder withB(UA ua);

        /**
         * Convenience method for calling first {@link Builder#build()} followed by {@link B2BUA#start()}.
         *
         * @return B2BUA
         */
        default B2BUA start() {
            final B2BUA ua = build();
            ua.start();
            return ua;
        }
    }

    interface RequestStream {
        RequestStream filter(Predicate<SipRequest> predicate);

        void doProcess(RequestProcessor processor);
    }

    interface ResponseStream {
        ResponseStream filter(Predicate<SipResponse> predicate);

        void doProcess(ResponseProcessor processor);
    }

    interface RequestProcessor {
        void process(B2BUA b2bua, SipRequest request, SipRequest.Builder builder);
    }

    interface ResponseProcessor {
        void process(B2BUA b2bua, SipResponse response, SipResponse builder);
    }
}

