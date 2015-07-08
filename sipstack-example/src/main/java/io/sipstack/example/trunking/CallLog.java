package io.sipstack.example.trunking;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import gov.nist.javax.sip.header.SIPHeader;
import io.pkts.packet.sip.header.SipHeader;

public class CallLog {

    public static final int TRUNKING_ORIGINATING_FLAG = 1 << 12; //Call.FLAG_SIP_TRUNKING_ORIGINATING;
    public static final int TRUNKING_TERMINATING_FLAG = 1 << 13; //Call.FLAG_SIP_TRUNKING_TERMINATING;
    public static final int TWILIO_CLIENT_2_0_FLAG = 1 << 14; //Call.FLAG_TWILIO_CLIENT_2_0;
    public static final int TRUNKING_VERIFICATION_CALL_FLAG = 1<<15; //Call.FLAG_SIP_TRUNKING_VERIFICATION_CALL;
    public static final int TRUNKING_SECURE = 1<<16; // Call.FLAG_SIP_TRUNKING_SECURE;

    public static final String DEFAULT_API_VERSION = "2010-04-01";

    private SipHeader callSidHeader;
    private Status status;
    private String AccountSid;
    private String From;
    private String FromUser;
    private String To;
    private LocalDateTime StartTime;
    private LocalDateTime EndTime;
    private LocalDateTime DateCreated;
    private LocalDateTime DateUpdated;
    private String SipCallId;
    private int Duration;
    private String ApiVersion;
    private String CalledVia;
    private String CallerIdName;
    private String PhoneNumberSid;
    private String ProviderSid;
    private int Flag;

    /**
     * Normally, price is set to null and it appears as TBD in the call log.
     * If we publish a billing event, the billing system will later overwrite the price field in post-flight DB. 
     * For verification calls, we set the price to $0 so that customers know they weren't billed.
     *  (Customers will see a $0 instead of TBD in the price field.)
     * The price will not be overwritten by billing event because we don't publish billing events for verification calls.
     */
    private Optional<BigDecimal> Price = Optional.<BigDecimal>empty();
    
    /**
     * Is this a verification call?
     */
    private boolean verificationCall = false;

    /**
     * Flag indicating whether or not we have posted this {@link CallLog} to PostFlight.
     */
    private boolean hasBeenReported;

    // This value holds whether a trunking call has been established or not for billing.
    // Even when the final status is not completed, we will send billing if the call has been established.
    // This value is set only when status changes to answered.
    private boolean hasEstablished;

    public CallLog(final SipHeader callSid) {
        this.callSidHeader = callSid;
        this.status = Status.UNDIALED;
        this.ApiVersion = DEFAULT_API_VERSION;
        this.DateCreated = LocalDateTime.now();
        this.StartTime = LocalDateTime.now();
        this.hasEstablished = false;
    }

    public SipHeader getCallSidHeader() {
        return callSidHeader;
    }

    public String getSid() {
        return callSidHeader.getValue().toString();
    }

    public Status getStatus() {
        return this.status;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public String getAccountSid() {
        return this.AccountSid;
    }

    public void setAccountSid(final SipHeader accountSid) {
        this.AccountSid = accountSid.getValue().toString();
    }

    public String getFrom() {
        return this.From;
    }

    public void setFrom(final String from) {
        this.From = from;
    }

    public String getFromUser() {
        return this.FromUser;
    }

    public void setFromUser(final String fromUser) {
        this.FromUser = fromUser;
    }

    public String getTo() {
        return this.To;
    }

    public void setTo(final String to) {
        this.To = to;
    }

    public String getSipCallId() {
        return this.SipCallId;
    }

    public void setSipCallId(final String sipCallId) {
        this.SipCallId = sipCallId;
    }

    public void setSipCallId(final SipHeader sipHeader) {
        this.SipCallId = sipHeader.getValue().toString();
    }

    public LocalDateTime getStartTime() {
        return this.StartTime;
    }

    public void markStartTime() {
        this.StartTime = LocalDateTime.now();
    }

    public LocalDateTime getEndTime() {
        return this.EndTime;
    }

    public void markEndTime() {
        this.EndTime = LocalDateTime.now();
    }

    public LocalDateTime getDateCreated() {
        return this.DateCreated;
    }

    public LocalDateTime getDateUpdated() {
        return this.DateUpdated;
    }

    public void markDateUpdated() {
        this.DateUpdated = LocalDateTime.now();
    }

    public String getApiVersion() {
        return this.ApiVersion;
    }

    public void setApiVersion(final SipHeader apiVersion) {
        this.ApiVersion = apiVersion.getValue().toString();
    }

    public String getCalledVia() {
        return this.CalledVia;
    }

    public void setCalledVia(final String calledVia) {
        this.CalledVia = calledVia;
    }

    public String getCallerIdName() {
        return this.CallerIdName;
    }

    public void setCallerIdName(final String callerIdName) {
        this.CallerIdName = callerIdName;
    }

    public String getPhoneNumberSid() {
        return this.PhoneNumberSid;
    }

    public void setPhoneNumberSid(final SipHeader phoneNumberSid) {
        this.PhoneNumberSid = phoneNumberSid.getValue().toString();
    }

    public int getDuration() {
        return this.Duration;
    }

    /**
     * Use {@link #isFlagEnabled(int)} instead to check if a certain flag has been set
     * @return
     */
    public int getFlag() {
        return this.Flag;
    }

    /**
     * Sets a flag value to the bitfield Flag
     * Adds a flag value to the bitfield if there's an existing flag 
     * @param flag
     */
    public void setFlag(final int flag) {
        this.Flag = (this.Flag == 0) ? flag : this.Flag | flag;
    }

    /**
     * Resets the bitfield Flag to the given flag value
     * @param flag
     */
    public void resetFlag(final int flag) {
        this.Flag = flag; 
    }
    
    /**
     * Checks if a certain flag has been set
     * @param flag
     * @return
     */
    public boolean isFlagEnabled(final int flag) {
        return (this.Flag & flag) == flag; 
    }

    public String getProviderSid() {
        return this.ProviderSid;
    }

    public void setProviderSid(final SipHeader providerSid) {
        this.ProviderSid = providerSid.getValue().toString();
    }

    public boolean hasEstablished() {
        return this.hasEstablished;
    }

    public void established() {
        this.hasEstablished = true;
    }
    
    public boolean isVerificationCall() {
        return this.verificationCall;
    }
    
    public void setVerificationCall(final boolean verificationCall) {
        this.verificationCall = verificationCall;
    }
    
    public BigDecimal getPrice() {
        return this.Price.orElse(null);
    }
    
    public void setPrice(final double price) {
        this.Price = Optional.<BigDecimal>of(new BigDecimal(price));
    }

    public void calculateAndSetDuration() throws IllegalArgumentException {
        if (this.EndTime == null) {
            this.EndTime = LocalDateTime.now();
        }

        // if call has never been established, set duration 0.
        // note that startTime cannot be null if hasEstablished is true.
        if (!this.hasEstablished) {
            this.Duration = 0;
        } else {
            this.Duration = (int) java.time.Duration.between(this.StartTime, this.EndTime).get(ChronoUnit.SECONDS);
        }
    }

    /**
     *
     * @return the hasBeenReported
     */
    public boolean getHasBeenReported() {
        return this.hasBeenReported;
    }

    /**
     * Mark this call log as reported.
     */
    public void callLogHasBeenReported() {
        this.hasBeenReported = true;
    }

    public boolean isFinalStatus() {
        return this.status.isTerminalState();
    }

    /*
     *     public Call(@JsonProperty("callSid") final String callSid,
                @JsonProperty("callSegmentSid") final String callSegmentSid,
                @JsonProperty("accountSid") final String accountSid,
                @JsonProperty("phoneNumberSid") final String phoneNumberSid,
                @JsonProperty("from") final String from,
                @JsonProperty("to") final String to,
                @JsonProperty("status") final Integer status,
                @JsonProperty("flags") final int flags,
                @JsonProperty("billableItemSid") final String billableItemSid,
                @JsonProperty("asteriskChannel") final String asteriskChannel,
                @JsonProperty("asteriskHostIp") final String ip,
                @JsonProperty("sipCallId") final String sipCallId,
                @JsonProperty("providerSid") final String providerSid,
                @JsonProperty("startTime") final DateTime startTime,
                @JsonProperty("endTime") final DateTime endTime,
                @JsonProperty("duration") final Integer duration,
                @JsonProperty("calledVia") final String calledVia,
                @JsonProperty("pricingModelSid") final String pricingModelSid,
                @JsonProperty("price") final BigDecimal price,
                @JsonProperty("dateCreated") final DateTime dateCreated,
                @JsonProperty("dateUpdated") final DateTime dateUpdated,
                @JsonProperty("billingReferenceTag") final String billingReferenceTag,
                @JsonProperty("callerIdName") final String callerIdName,
                @JsonProperty("parentCallSid") final String parentCallSid,
                @JsonProperty("groupSid") final String groupSid,
                @JsonProperty("apiVersion") final String apiVersion) {
     */
//    public Call getCall() {
//        return new Call(this.sid, "", this.AccountSid, this.PhoneNumberSid, this.From, this.To, this.status.getStatusCode(), this.Flag,
//                null, null, null, this.SipCallId, this.ProviderSid, this.StartTime, this.EndTime, this.Duration,
//                this.CalledVia, null, this.Price.orNull(), this.DateCreated, this.DateUpdated, null, this.CallerIdName, null, null,
//                this.ApiVersion);
//    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Sid=" + getSid() + ",");
        sb.append("AccountSid=" + this.AccountSid + ",");
        sb.append("PhoneNumberSid=" + this.PhoneNumberSid + ",");
        sb.append("From=" + this.From + ",");
        sb.append("To=" + this.To + ",");
        sb.append("Status=" + this.status + ",");
        sb.append("Flag=" + this.Flag + ",");
        sb.append("SipCallId=" + this.SipCallId + ",");
        sb.append("ProviderSid=" + this.ProviderSid + ",");
        sb.append("StartTime=" + this.StartTime + ",");
        sb.append("EndTime=" + this.EndTime + ",");
        sb.append("Duration=" + this.Duration + ",");
        sb.append("Price=" + this.Price.orElse(null) + ",");
        sb.append("DateCreated" + this.DateCreated + ",");
        sb.append("DateUpdated=" + this.DateUpdated + ",");
        sb.append("CalledVia=" + this.CalledVia + ",");
        sb.append("CallerIdName=" + this.CallerIdName + ",");
        sb.append("ApiVersion=" + this.ApiVersion + ",");
        sb.append("HasEstablished=" + this.hasEstablished + ",");
        sb.append("Verification=" + this.verificationCall + ",");
        return sb.toString();
    }

    public enum Status {
        UNDIALED(0, false),
        ANSWERED(1, false),
        COMPLETED(2, true),
        BUSY(3, true),
        FAIL(4, true),
        NO_ANSWER(5, true),
        RINGING(6, false);

        private int value;
        private boolean terminalState;

        Status(final int value, final boolean terminalState) {
            this.value = value;
            this.terminalState = terminalState;
        }

        public int getStatusCode() {
            return this.value;
        }

        public boolean isTerminalState() {
            return this.terminalState;
        }
    }

}