package io.sipstack.example.trunking;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.pkts.buffer.Buffer;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipParseException;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.pkts.packet.sip.address.Address;
import io.pkts.packet.sip.address.SipURI;
import io.pkts.packet.sip.header.AddressParametersHeader;
import io.pkts.packet.sip.header.FromHeader;
import io.pkts.packet.sip.header.SipHeader;
import io.sipstack.application.ApplicationInstance;
import io.sipstack.application.B2BUA;
import io.sipstack.application.SipRequestEvent;
import io.sipstack.application.UA;

public class TrunkingServiceApplicationInstance extends ApplicationInstance {

    private static final Logger logger = LoggerFactory.getLogger(TrunkingServiceApplicationInstance.class);

    private final CallLog callLog;

    public TrunkingServiceApplicationInstance(final Buffer id, final SipMessage message) {
        super(id);

        final Optional<SipHeader> callSidHeader = message.getHeader(TwilioHeaders.X_TWILIO_CALLSID_HEADER);
        this.callLog = new CallLog(callSidHeader.get());

        // TODO this.metrics.sessionCreated();
        logInfo("Session created");
    }

    @Override
    public void onRequest(final SipRequestEvent event) {
        if (event.message().isInvite()) {
            doInitialInvite(event);
        } else {
            super.onRequest(event);
        }
    }

    private void doInitialInvite(final SipRequestEvent event) {
        final SipRequest request = event.message().toRequest();
        logInfo(request, "Received initial invite. Request URI: " + request.getRequestUri());

        final UA uaA = uaWithFriendlyName("A").withRequest(event).build();

        if (request.getMaxForwards().getMaxForwards() == 0) {
            logInfo(request, "Max-Forwards header of incoming INVITE is zero. Returning 483 and killing the call.");
            rejectCall(uaA, request, 483, null);
            return;
        }

        final UA uaB = uaWithFriendlyName("B").withTarget(request.getRequestUri()).build();
        final B2BUA b2b = b2buaWithFriendlyName("b2bua").withA(uaA).withB(uaB).build();

        b2b.onRequest().filter(r -> r.isInitial() && r.isInvite()).doProcess(this::onInitialInvite);
        b2b.onRequest().filter(SipRequest::isCancel).doProcess(this::onCancelRequest);
        b2b.onRequest().filter(SipRequest::isBye).doProcess(this::onByeRequest);

        b2b.onResponse().filter(r -> r.isInitial() && r.isSuccess()).doProcess(this::onInitialSuccessResponse);
        b2b.onResponse().filter(r -> r.isInitial() && r.isError()).doProcess(this::onInitialErrorResponse);

        b2b.onResponse().filter(SipResponse::isBye).doProcess(this::onByeResponse);
        b2b.onResponse().filter(SipResponse::isInfo).doProcess(this::onInfoResponse);

        b2b.start();
    }

    private void onInitialInvite(final B2BUA b2bua, final SipRequest request, final SipRequest.Builder builder) {

        try {
            request.getHeader(TwilioHeaders.X_TWILIO_ACCOUNT_SID_HEADER).ifPresent(callLog::setAccountSid);
            request.getHeader(TwilioHeaders.X_ORIGINAL_CALL_ID).ifPresent(callLog::setSipCallId);
            request.getHeader(TwilioHeaders.X_TWILIO_API_VERSION_HEADER).ifPresent(callLog::setApiVersion);
            request.getHeader(TwilioHeaders.X_TWILIO_PHONENUMBER_SID).ifPresent(callLog::setPhoneNumberSid);
            request.getHeader(TwilioHeaders.X_TWILIO_PROVIDER_SID).ifPresent(callLog::setProviderSid);

            // check if this is a verification call
            checkVerificationCall(request);

            // TODO copy headers, except "Remote-Party-ID" or TwilioHeaders.X_TWILIO_VERIFICATION_CALL

            // Set From, which is either in RPI or From header. If From address contains E.164 in
            // user part, only put user part. Otherwise, put the SIP URI without parameters.
            // TODO: we might need to see P-Preferred-Identity to determine the true callerid, but
            // ignore it at this point.
            final Optional<SipHeader> remotePartyIdHeader = request.getHeader("Remote-Party-ID");
            final Address remoteAddress;
            if (remotePartyIdHeader.isPresent()) {
                //TODO
                //remoteAddress = ((AddressParametersHeader) remotePartyIdHeader.get()).getAddress();
                //patchCallerId(remoteAddress, request, builder);
            } else {
                remoteAddress = request.getFromHeader().getAddress();
            }

            //TODO
            //callLog.setFrom(parseCallerId(remoteAddress));
            //callLog.setFromUser(((SipURI) remoteAddress.getURI()).getUser().toString());
            //callLog.setCallerIdName(remoteAddress.getDisplayName().toString());

            final EdgeType edgeType = parseEdgeType(request);
            if (edgeType == EdgeType.PHONE) {
                processOriginatingInvite(request, builder);
            } else if (edgeType == EdgeType.PUBLIC_SIP) {
                // If user part is not E.164 number, throw exception
                // NOTE: this check has to go last. If it doesn't then
                // we will not fill out all parts of the CallLog, which
                // will be an issue if we bail on the check below.
                // See VoiceCon-390
                // TODO
                //final SipURI requestURI = (SipURI) linkedRequest.getRequestURI();
                //if (!SipUtil.isValidE164Number(requestURI.getUser())) {
                //    rejectCall(request, 400, "Invalid phone number");
                //}

                processTerminatingInvite(request, builder);
            } else {
                throw new SipParseException("Unsupported edge type " + edgeType);
            }

            builder.header(SipHeader.create(TwilioHeaders.X_TWILIO_DIRECTION_HEADER, "outbound"));
        } catch (final SipParseException e) {
            logWarn(request, e.getMessage(), e);
            rejectCall(b2bua.getUaA(), request, 400, null);
        } catch (final Exception e) {
            logError(request, "Error", e);
            rejectCall(b2bua.getUaA(), request, 500, null);
        }
    }

    private void onCancelRequest(final B2BUA b2bua, final SipRequest request, final SipRequest.Builder builder) {
        updateStatus(request, CallLog.Status.NO_ANSWER);
    }

    private void onByeRequest(final B2BUA b2bua, final SipRequest request, final SipRequest.Builder builder) {
        // TODO: should handle BYE in transfer case!!!!

        updateStatus(request, CallLog.Status.COMPLETED);

        // B2BUA BYE
        //copyByeHeaders(request, builder);
    }

    private void checkVerificationCall(final SipMessage message) {
        if (message.getHeader(TwilioHeaders.X_TWILIO_VERIFICATION_CALL).isPresent()) {
            callLog.setVerificationCall(true);
            callLog.setFlag(CallLog.TRUNKING_VERIFICATION_CALL_FLAG);
            callLog.setPrice(0.0);
        }
    }

    private void rejectCall(final UA uaA, final SipRequest invite, final int statusCode, final String reason) {
        // TODO how to set reason phrase?
        final SipResponse response = invite.createResponse(statusCode);
        updateStatus(response, CallLog.Status.FAIL);
        uaA.send(response);
    }

    /**
     * Handle INVITE request of originating case.
     */
    private void processOriginatingInvite(final SipRequest request, final SipRequest.Builder linked) {

        final SipURI requestURI = linked.requestURI();

        final Optional<SipHeader> trunkSidHeader = request.getHeader(TwilioHeaders.X_TWILIO_TRUNK_SID);
        if (trunkSidHeader.isPresent()) {
            // If we have a trunk sid proxy core supported the 'trunk' user parameter
            requestURI.setParameter("user", "trunk");
        } else {
            requestURI.setParameter("public-sip", "trunk");
            linked.header(SipHeader.create("X-Twilio-OutboundRequestUri0", requestURI.toString()));
            linked.header(SipHeader.create("X-Twilio-OutboundRouteCount", "1"));
        }

        // Set the From header in the forked INVITE as {caller's number}@{customer's trunking domain}.
        // This makes a callback use our Twilio terminating.
//        final SipURI fromURI = (SipURI) linked.from().getAddress().getURI();
//        fromURI.setHost(getDefaultTrunkingDomain(callLog.getAccountSid()));

        // In originating, if the callerid is not E.164, set it as unknown
        // This is not to reveal the sender's SIP URI in short-circuiting case.
//        if (!SipUtil.isValidE164Number(callLog.getFrom())) {
//            callLog.setFrom(UNKNOWN_CALLERID);
//        }

        // Set user part of To header as "sipout". This is what PMG expects for outbound.
        final Address toAddress = linked.to().getAddress();
//        toAddress.setDisplayName("sipout");
        final SipURI toURI = (SipURI) toAddress.getURI();
        toURI.setParameter("user", "sipout");

        // Set to field in calllog, which is SIP URI in request-URI
        callLog.setTo(parseSipUri((SipURI) request.getRequestUri()));

        // Set calledvia if there is Diversion header. This is twilio number and used as To field in billing.
        if (request.getHeader("Diversion").isPresent()) {
            try {
                //TODO
                //final String user = getSipURIFromHeader(request, "Diversion").getUser().toString();
                //callLog.setCalledVia(user);
                //logInfo(request, "Processing trunking-originating to " + user);
            } catch (final Exception e) {
                logger.warn("Unable to parse the Diversion header. Pls check so that the billing for this call "
                        + callLog.getSid() + " is correct", e);
            }
        }

        callLog.setFlag(CallLog.TRUNKING_ORIGINATING_FLAG);
    }

    /**
     * Handle the INVITE of terminating case.
     */
    private void processTerminatingInvite(final SipRequest request, final SipRequest.Builder linked) {

        logInfo(request, "Processing trunking-terminating to " + request.getRequestUri());

        // Set To, user part (PSTN) in X-Twilio-Request-URI
        final Optional<SipHeader> toAddress = request.getHeader(TwilioHeaders.X_TWILIO_REQUEST_URI);
        toAddress.ifPresent(h -> {
            // TODO
            //final AddressParametersHeader addressHeader = (AddressParametersHeader) h;
            //this.callLog.setTo(parseCallerId(addressHeader.getAddress()));
        });
        this.callLog.setFlag(CallLog.TRUNKING_TERMINATING_FLAG);

//        final SipURI uri = (SipURI) request.getRequestUri().clone();
//        uri.setParameter("user", "phone");
        linked.requestURI().setParameter("user", "phone");
    }

    /**
     * Fix of voicecon-377.
     *
     * Currently, the From-address is removed by the public- and carrier-media-gateawy and replaced
     * with 'sipin' and 'gmg'. The reason for this is stupid Asterisk behavior where it matches the
     * incoming request to a particular context based on the From-address and since we needed
     * different behavior depending on where it came from, we had to do it in this way. However, for
     * trunking we must re-write the From-address back to what it was and remote the Remote-Party-ID
     * header removed.
     *
     * NOTE: in the future we may actually also have to stamp P-Asserted-Identity etc.
     *
     * NOTE2: once we have moved all traffic over to carrier-media-gateway we no longer need to do
     * the special from-header handling since all traffic to and from Hurl (Asterisk) will be going
     * through the proxy-core and therefore only one provider as far as Asterisk is concerned.
     *
     * @param remoteAddress this is the actual remote address as the original incoming request to
     *        CMG or PMG was. This is "calculated" before calling this method and is typically the
     *        Remote-Party-ID header.
     * @param original Original request
     * @param linked the "b2bua:ed" request, whose From-address we are patching.
     */
    private void patchCallerId(final Address remoteAddress, final SipRequest original, final SipRequest.Builder linked) {
        final Address fromAddress = Address.with(original.getFromHeader().getAddress().getURI())
                .displayName(remoteAddress.getDisplayName())
                .build();
        final FromHeader from = FromHeader.with(fromAddress)
                .user(((SipURI) remoteAddress.getURI()).getUser())
                .build();
        linked.from(from);
    }

    private void doInfo(final SipRequest request) {
//        final TwilioB2buaHelper b2buaHelper = new TwilioB2buaHelper(request);
//        final SipSession linkedSession = b2buaHelper.getLinkedSession(request.getSession());
//        if (linkedSession != null) {
//            try {
//                final SipRequest linkedRequest = linkedSession.createRequest("INFO");
//                linkedRequest.setAttribute(ORIGINAL_REQ, request);
//                linkedRequest.setContent(request.getContent(), request.getContentType());
//                copyHeaders(request, linkedRequest, ImmutableList.of("Info-Package", "Content-Disposition"));
//                sendSipMessage(linkedRequest);
//            } catch (final Exception e) {
//                logInfo(linkedSession.getApplicationSession(), "Unable to create INFO message: " + e.getMessage());
//                sendSipMessage(request.createResponse(200));
//            }
//        }
    }

    private void doRefer(final SipRequest request) {
//        final SipApplicationSession sas = request.getApplicationSession(false);
//        if (sas != null) {
//            final TwilioB2buaHelper b2buaHelper = new TwilioB2buaHelper(request);
//            final SipSession linkedSession = b2buaHelper.getLinkedSession(request.getSession());
//            final String referTo = request.getHeader("Refer-To");
//            final String callSid = getCallSid();
//            logDebug(request, "Received refer target: " + referTo);
//
//            // reply to transferer with 202
//            final SipResponse response = request.createResponse(202);
//            sendSipMessage(response);
//
//            // create and send invite with no SDP to transfer target
//            final SipRequest createdRequest =
//                    this.sipFactory.createRequest(sas, "INVITE", linkedSession.getRemoteParty(),
//                            this.sipFactory.createAddress(referTo));
//            // createdRequest.pushRoute(getNextHopURI());
//            sendSipMessage(createdRequest);
//
//            // store transfer state
//            final CallLog transferCallLog = new CallLog(callSid);
//            transferCallLog.setStatus(Status.UNDIALED);
//            sas.setAttribute(SessionAttributes.TRANSFER_TRANSFERER_SESSION, request.getSession());
//            sas.setAttribute(SessionAttributes.TRANSFER_TRANSFEREE_SESSION, linkedSession);
//            sas.setAttribute(SessionAttributes.TRANSFER_TARGET_SESSION, createdRequest.getSession());
//            sas.setAttribute(SessionAttributes.TRANSFER_CALLLOG, transferCallLog);
//        }
    }

    private void onInitialSuccessResponse(final B2BUA b2bua, final SipResponse response, final SipResponse builder) {
        if (true) { // transferCallLog == null)
            updateStatus(response, CallLog.Status.ANSWERED);
            checkVerificationCall(response);
            // If this is trunking-terminating, try to update ProviderSid
            if (callLog.isFlagEnabled(CallLog.TRUNKING_TERMINATING_FLAG)) {
                final Optional<SipHeader> providerSid = response.getHeader(TwilioHeaders.X_TWILIO_PROVIDER_SID);
                providerSid.ifPresent(callLog::setProviderSid);
            }
        } else {
            // Transfer case
//            if (transferCallLog.getStatus() == Status.RINGING) {
//                // When receiving 200 from transfer-target, send re-INVITE to transferee
//                final SipSession
//                        transfereeSession =
//                        (SipSession) sas.getAttribute(SessionAttributes.TRANSFER_TRANSFEREE_SESSION);
//                final SipRequest reInviteRequest = transfereeSession.createRequest("INVITE");
//                sendSipMessage(reInviteRequest);
//                transferCallLog.setStatus(Status.ANSWERED);
//            } else if (transferCallLog.getStatus() == Status.ANSWERED) {
//                // When receiving 200 from transferee for re-INVITE
//                // TODO: send ACK to the target
//                sas.removeAttribute(SessionAttributes.TRANSFER_CALLLOG);
//            }
        }
    }

    private void onInitialErrorResponse(final B2BUA b2bua, final SipResponse response, final SipResponse builder) {
        logInfo(response, "Received final response. Call is terminated.");

        if (true) { // transferCallLog == null)

            // Regular case
            CallLog.Status callLogStatus = CallLog.Status.FAIL;
            if (response.getStatus() == 486) { // Busy Here
                callLogStatus = CallLog.Status.BUSY;
            }
    
            updateStatus(response, callLogStatus);
        } else {
            // Transfer case
//            if (transferCallLog.getStatus() == Status.UNDIALED || transferCallLog.getStatus() == Status.RINGING) {
//                // When receiving error from transfer-target,
//                // TODO: send NOTIFY to transferer
//            }
        }
    }

    private void onByeResponse(final B2BUA b2bua, final SipResponse response, final SipResponse builder) {
        logInfo(response, "Received final response. Call is terminated.");
//        copyByeHeaders(response, builder);
    }

    private void onInfoResponse(final B2BUA b2bua, final SipResponse response, final SipResponse builder) {
//        final SipRequest originalRequest = (SipRequest) response.getRequest().getAttribute(ORIGINAL_REQ);
//        final SipResponse r = originalRequest.createResponse(200);
//        sendSipMessage(r);
    }

    private void doProvisionalResponse(final SipResponse response) {
//        final SipApplicationSession sas = response.getApplicationSession(false);
//        if (sas != null) {
//            final int statusCode = response.getStatus();
//            final String method = response.getMethod();
//            if (method.equals("INVITE") && statusCode >= 180 && statusCode <= 183) {
//                if (sas.getAttribute(SessionAttributes.TRANSFER_CALLLOG) == null) {
//                    // Regular case
//                    if (updateStatus(response, Status.RINGING)) {
//                        checkVerificationCall(response);
//                        final SipResponse linkedResponse = createLinkedResponseToInvite(response);
//                        if (linkedResponse != null) {
//                            final MediaSession mediaSession = getMediaSession(sas);
//                            if (mediaSession != null && APPLICATION_SDP.equals(response.getContentType())) {
//                                final MediaSession.AnswerBuilder answer = mediaSession.withAnswer(response.getContent())
//                                        .withProvisional(true);
//                                Futures.addCallback(answer.send(), new FutureCallback<SdpResponse>() {
//                                    @Override
//                                    public void onSuccess(final SdpResponse result) {
//                                        try {
//                                            linkedResponse.setContent(result.getSdp(), APPLICATION_SDP);
//                                        } catch (final UnsupportedEncodingException e) {
//                                            logWarn(response, "Failed to update SDP", e);
//                                        }
//                                        sendSipMessage(linkedResponse);
//                                    }
//
//                                    @Override
//                                    public void onFailure(final Throwable t) {
//                                        logWarn(response, "Media server error on provisional response (ignored)", t);
//                                    }
//                                });
//                            } else {
//                                sendSipMessage(linkedResponse);
//                            }
//                        }
//                    } else {
//                        logInfo(response, "Received provisional response at invalid status so dropping it.");
//                    }
//                } else {
//                    // Transfer case
//                    final CallLog transferCallLog = (CallLog) sas.getAttribute(SessionAttributes.TRANSFER_CALLLOG);
//                    if (transferCallLog.getStatus() == Status.UNDIALED) {
//                        // TODO: Send notify to the tranferer
//                        transferCallLog.setStatus(Status.RINGING);
//                    }
//                }
//            }
//        }
    }

    /**
     * This method updates the call status. Also call terminateCall() if status is final.
     *
     * @param message
     * @param status
     * @return
     */
    private void updateStatus(final SipMessage message, final CallLog.Status status) {
        // update only current status is not final
        if (!callLog.isFinalStatus()) {
            if (callLog.getStatus() != status) {
                final String msg = "Changed status : " + callLog.getStatus() + " -> " + status;
                if (message != null) {
                    logInfo(message, msg);
                } else {
                    logInfo(msg);
                }
                // If state is answered, update start time so we can calculate call duration
                // accurately. Also set hasEstablished flag for billing later.
                if (status == CallLog.Status.ANSWERED) {
                    callLog.markStartTime();
                    callLog.established();
                }

                callLog.markDateUpdated();
                callLog.setStatus(status);

                // If new status is final, terminate call
                if (callLog.isFinalStatus()) {
                    completeCall();
                }
            }
        }
    }

    /**
     * This method terminates the call by sending postflight and billing request.
     */
    private void completeCall() {
        callLog.markEndTime();
        try {
            callLog.calculateAndSetDuration();
            logInfo(callLog, "Call terminated with status " + callLog.getStatus() + " Duration of call (sec) " + callLog.getDuration());
            callLog.callLogHasBeenReported();
            callPostflight();

            // send billing event only if call has been established and call duration is not 0.
            // so we bill even when the final status is not completed, but has established before.
            if (callLog.hasEstablished() && callLog.getDuration() > 0 && !callLog.isVerificationCall()) {
                callBilling();
            } else {
                logInfo(callLog, "Not sending billing event because call status has not been established or duration is 0.");
            }
        } catch (final IllegalArgumentException e) {
            logWarn(callLog, "Failed to calculate call duration (sec): " + callLog);
        }
    }

    /**
     * This method sends a request to the postflight-voice-api to write the call log. Invoked when
     * the call state reaches to the final state.
     */
    private void callPostflight() {
//        if (this.postflight == null) {
//            logWarn(sas, callLog, "Not sending postflight request because it is disabled");
//            return;
//        }
//
//        final String sasId = sas.getId();
//        final Call call = callLog.getCall();
//        final ListenableFuture<Void> resp = this.postflight.writeFinalCall(callLog.getAccountSid(), call);
//        // Note that application session might be already invalidated when callback is invoked.
//        // So don't try to access application session.
//        Futures.addCallback(resp, new FutureCallback<Void>() {
//            @Override
//            public void onSuccess(final Void result) {
//                logger.info(buildLogMessage(sasId, callLog, "Successfully written to postflight.\n"
//                        + PostflightUtils.convertCallToJson(call)));
//            }
//
//            @Override
//            public void onFailure(final Throwable t) {
//                logger.warn(buildLogMessage(sasId, callLog, "Received error from postflight. Error message: "
//                        + t.getMessage() + "\n"
//                        + PostflightUtils.generatePostflightUrl(callLog.getAccountSid(), callLog.getSid()) + "\n"
//                        + PostflightUtils.convertCallToJson(call)));
//            }
//        });
    }

    /**
     * This method sends a billing event to AWS SQS billing queue. Invoked at the end of each call.
     */
    private void callBilling() {
//        if (this.billing == null) {
//            logWarn(sas, callLog, "Not sending billing event because it is disabled");
//            return;
//        }
//
//        final String sasId = sas.getId();
//        final List<VoiceBillingEvent> billingEvents = BillingUtils.convertToBillingEvents(callLog);
//        for (final VoiceBillingEvent billingEvent : billingEvents) {
//            final ListenableFuture<String> future = this.billing.sendMessageAsync(billingEvent);
//            Futures.addCallback(future, new FutureCallback<String>() {
//                @Override
//                public void onSuccess(final String result) {
//                    logger.info(buildLogMessage(sasId, callLog, "Successfully sent billing event. MessageId: "
//                            + result + "\n"
//                            + billingEvent.toXml()));
//                }
//
//                @Override
//                public void onFailure(final Throwable t) {
//                    logger.warn(buildLogMessage(sasId, callLog, "Received error from billing. Error message: "
//                            + t.getMessage() + "\n"
//                            + billingEvent.toXml()));
//                }
//            });
//        }
    }

    /**
     * Helper to read callsid.
     * @return CallSid
     */
    private String getCallSid() {
        return callLog.getSid();
    }

    /**
     * Helper to read a SIP header in the request and return as SipURI format
     *
     * @param request Request
     * @param headerName Header name
     * @return Sip URI
     */
    private SipURI getSipURIFromHeader(final SipRequest request, final String headerName) {
        final Optional<SipHeader> header = request.getHeader(headerName);
        if (header.isPresent() && header.get() instanceof AddressParametersHeader) {
            return (SipURI) ((AddressParametersHeader) header.get()).getAddress().getURI();
        } else {
            throw new SipParseException("Header " + headerName + " is not set or does not contain a SIP URI");
        }
    }

    /**
     * Helper to extract caller information from SIP address. If the user part of the address is
     * E.164, return only the user part. Otherwise, return the SIP URI.
     *
     * @param address Address
     * @return
     */
    private String parseCallerId(final Address address) {
        final SipURI uri = (SipURI) address.getURI();
        return uri.getUser().toString();
//        if (SipUtil.isValidE164Number(uri.getUser())) {
//            return uri.getUser();
//        } else {
//            return parseSipUri(uri);
//        }
    }

    /**
     * Helper to extract SIP URI without any parameters.
     *
     * @param uri SIP URI
     * @return
     */
    private String parseSipUri(final SipURI uri) {
        final StringBuilder sb = new StringBuilder("sip:");
        sb.append(uri.getUser()).append("@").append(uri.getHost());
        if (uri.getPort() != -1 && uri.getPort() != 5060) {
            sb.append(":").append(uri.getPort());
        }
        return sb.toString();
    }

    /**
     * Helper to read the user parameter in the SIP From header.
     *
     * @param sipMessage SIP message
     * @return
     * @throws IllegalArgumentException
     */
    private EdgeType parseEdgeType(final SipMessage sipMessage) throws IllegalArgumentException {
        final String user = ((SipURI) sipMessage.getFromHeader().getAddress().getURI()).getUserParam().toString();
        return EdgeType.parse(user);
    }

    /**
     * Copy headers in the BYE or the response of BYE to the b2buaed message. At this point, we only
     * copy X-Twilio-RecordingSid and X-Twilio-RecordingDuration that is configured in yaml.
     *
     * @param from
     * @param to
     */
    private void copyByeHeaders(final SipMessage from, final SipMessage to) {
//        copyHeaders(from, to, this.config.getCopyByeHeaders());
    }

    /**
     * Copy headers from original message to the b2buaed message.
     *
     * @param from
     * @param to
     * @param h
     */
    private void copyHeaders(final SipMessage from, final SipMessage to, final List<String> h) {
        for (final String header : h) {
            from.getHeader(header).ifPresent(to::addHeader);
        }
    }

    // TODO
    private String getDefaultTrunkingDomain(final String accountSid) {
        return "pstn.twilio.com";
    }

    private String buildLogMessage(final String msg) {
        return String.format("[%s] %s", id(), msg);
    }

    private String buildLogMessage(final CallLog callLog, final String msg) {
        return String.format("[%s,%d,%s,%s] %s", id(), 0, callLog.getSid(), callLog.getSipCallId(), msg);
    }

    private String buildLogMessage(final SipMessage message, final String str) {
        return String.format("[%s,%s,%d,%s,%s] %s", id(), message.getMethod(),
                message instanceof SipResponse ? ((SipResponse) message).getStatus() : 0,
                        getCallSid(), message.getCallIDHeader().getCallId(), str);
    }

    private void logInfo(final SipMessage message, final String str) {
        if (logger.isInfoEnabled()) {
            logger.info(buildLogMessage(message, str));
        }
    }

    private void logWarn(final String str, final Throwable t) {
        logger.warn(buildLogMessage(str), t);
    }

    private void logWarn(final String str) {
        logger.warn(buildLogMessage(str));
    }

    private void logWarn(final CallLog callLog, final String str) {
        logger.warn(buildLogMessage(callLog, str));
    }

    private void logInfo(final CallLog callLog, final String str) {
        logger.info(buildLogMessage(callLog, str));
    }

    private void logInfo(final String str) {
        logger.info(buildLogMessage(str));
    }

    private void logWarn(final SipMessage message, final String str, final Throwable t) {
        logger.warn(buildLogMessage(message, str), t);
    }

    private void logError(final SipMessage message, final String str, final Throwable t) {
        logger.error(buildLogMessage(message, str), t);
    }
}
