package io.sipstack.example.trunking;

/**
 * Created by ajansson on 7/6/15.
 */
public interface TwilioHeaders {
    String X_TWILIO_CALLSID_HEADER = "X-Twilio-CallSid";
    String X_TWILIO_ACCOUNT_SID_HEADER = "X-Twilio-AccountSid";
    String X_TWILIO_API_VERSION_HEADER = "X-Twilio-ApiVersion";
    String X_ORIGINAL_CALL_ID = "X-Twilio-Orig-Call-ID";
    String X_TWILIO_DIRECTION_HEADER = "X-Twilio-Direction";
    String X_TWILIO_PROVIDER_SID = "X-Twilio-ProviderSid";
    String X_TWILIO_PHONENUMBER_SID = "X-Twilio-PhoneNumberSid";
    String X_TWILIO_VERIFICATION_CALL = "X-Twilio-VerificationCall";
    String X_TWILIO_TRUNK_SID = "X-Twilio-TrunkSid";
    String X_TWILIO_REQUEST_URI = "X-Twilio-Request-URI";
}