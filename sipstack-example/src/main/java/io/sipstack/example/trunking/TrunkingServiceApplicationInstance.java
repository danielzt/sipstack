package io.sipstack.example.trunking;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
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

public class TrunkingServiceApplicationInstance extends ApplicationInstance {

    /**
     * The attribute name under which we will store the original request that
     * initiated this transaction.
     */
    public static final String ORIGINAL_REQ = "original_req";

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
    public void onRequest(final SipRequest request) {
        if (request.isInitial() && request.isInvite()) {
            doInitialInvite(request);
        } else {
            super.onRequest(request);
        }
    }

    @Override
    public void onResponse(final SipResponse response) {
        super.onResponse(response);
    }

    private void doInitialInvite(final SipRequest request) {
        logInfo(request, "Received initial invite. Request URI: " + request.getRequestUri());

        if (request.getMaxForwards().getMaxForwards() == 0) {
            logInfo(request, "Max-Forwards header of incoming INVITE is zero. Returning 483 and killing the call.");
            rejectCall(request, 483, null);
            return;
        }

        // check if this is a verification call
        checkVerificationCall(request);

        request.getHeader(TwilioHeaders.X_TWILIO_ACCOUNT_SID_HEADER).ifPresent(callLog::setAccountSid);
        request.getHeader(TwilioHeaders.X_ORIGINAL_CALL_ID).ifPresent(callLog::setSipCallId);
        request.getHeader(TwilioHeaders.X_TWILIO_API_VERSION_HEADER).ifPresent(callLog::setApiVersion);
        request.getHeader(TwilioHeaders.X_TWILIO_PHONENUMBER_SID).ifPresent(callLog::setPhoneNumberSid);
        request.getHeader(TwilioHeaders.X_TWILIO_PROVIDER_SID).ifPresent(callLog::setProviderSid);

        try {
            // Create forked leg
            final SipRequest.Builder linked = SipRequest.invite(request.getRequestUri());

            // TODO copy headers, except "Remote-Party-ID" or TwilioHeaders.X_TWILIO_VERIFICATION_CALL

            // Set From, which is either in RPI or From header. If From address contains E.164 in
            // user part, only put user part. Otherwise, put the SIP URI without parameters.
            // TODO: we might need to see P-Preferred-Identity to determine the true callerid, but
            // ignore it at this point.
            final Address remoteAddress;

            final Optional<SipHeader> remotePartyIdHeader = request.getHeader("Remote-Party-ID");
            if (remotePartyIdHeader.isPresent()) {
                remoteAddress = ((AddressParametersHeader) remotePartyIdHeader.get()).getAddress();
                patchCallerId(remoteAddress, request, linked);
            } else {
                remoteAddress = request.getFromHeader().getAddress();
            }

            callLog.setFrom(parseCallerId(remoteAddress));
            callLog.setFromUser(((SipURI) remoteAddress.getURI()).getUser().toString());
            callLog.setCallerIdName(remoteAddress.getDisplayName().toString());

            final EdgeType edgeType = parseEdgeType(request);
            if (edgeType == EdgeType.PHONE) {
                processOriginatingInvite(request, linked, callLog);
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

                processTerminatingInvite(request, linked, callLog);
            } else {
                throw new SipParseException("Unsupported edge type " + edgeType);
            }

            final SipRequest linkedRequest = linked.build();
            linkedRequest.setHeader(SipHeader.create(TwilioHeaders.X_TWILIO_DIRECTION_HEADER, "outbound"));

            // Send the forked INVITE back to proxy-core
            sendSipMessage(linkedRequest);
        } catch (final SipParseException e) {
            logWarn(request, e.getMessage(), e);
            rejectCall(request, 400, null);
        } catch (final Exception e) {
            logError(request, "Error message: " + e.getMessage(), e);
            rejectCall(request, 500, null);
        }
    }

    private void rejectCall(final SipRequest invite, final int statusCode, final String reason) {
        // TODO how to set reason phrase?
        final SipResponse response = invite.createResponse(statusCode);
        updateStatus(response, CallLog.Status.FAIL);
        sendSipMessage(response);
    }

    /**
     * Handle INVITE request of originating case.
     *
     * @param linkedRequest
     * @param callLog
     */
    private SipMessage processOriginatingInvite(final SipRequest request, final SipRequest linkedRequest, final CallLog callLog) {

        final SipURI requestURI = (SipURI) linkedRequest.getRequestURI();

        final String trunkSidHeader = request.getHeader(TwilioHeaders.X_TWILIO_TRUNK_SID);
        if (trunkSidHeader !=null) {
            // If we have a trunk sid proxy core supported the 'trunk' user parameter
            requestURI.setUserParam("trunk");
        } else {
            requestURI.setUserParam("public-sip");
            linkedRequest.setHeader("X-Twilio-OutboundRequestUri0", requestURI.toString());
            linkedRequest.setHeader("X-Twilio-OutboundRouteCount", "1");
        }

        // Set the From header in the forked INVITE as {caller's number}@{customer's trunking domain}.
        // This makes a callback use our Twilio terminating.
        final SipURI fromURI = (SipURI) linkedRequest.getFrom().getURI();
        fromURI.setHost(getDefaultTrunkingDomain(callLog.getAccountSid()));

        // In originating, if the callerid is not E.164, set it as unknown
        // This is not to reveal the sender's SIP URI in short-circuiting case.
        if (!SipUtil.isValidE164Number(callLog.getFrom())) {
            callLog.setFrom(UNKNOWN_CALLERID);
        }

        // Set user part of To header as "sipout". This is what PMG expects for outbound.
        final Address toAddress = linkedRequest.getTo();
        toAddress.setDisplayName("sipout");
        final SipURI toURI = (SipURI) toAddress.getURI();
        toURI.setUser("sipout");

        // Set to field in calllog, which is SIP URI in request-URI
        callLog.setTo(parseSipUri((SipURI) request.getRequestURI()));

        // Set calledvia if there is Diversion header. This is twilio number and used as To field in billing.
        if (linkedRequest.getHeader("Diversion") != null) {
            try {
                final String user = getSipURIFromHeader(linkedRequest, "Diversion").getUser();
                callLog.setCalledVia(user);
                logInfo(request, "Processing trunking-originating to " + user);
            } catch (final Exception e) {
                logger.warn("Unable to parse the Diversion header. Pls check so that the billing for this call "
                        + callLog.getSid() + " is correct", e);
            }
        }

        callLog.setFlag(CallLog.TRUNKING_ORIGINATING_FLAG);
        return linkedRequest;
    }

    /**
     * Handle the INVITE of terminating case.
     */
    private void processTerminatingInvite(final SipRequest request, final SipRequest.Builder linked) {

        logInfo(request, "Processing trunking-terminating to " + request.getRequestUri());

        // Set To, user part (PSTN) in X-Twilio-Request-URI
        final Optional<SipHeader> toAddress = request.getHeader(TwilioHeaders.X_TWILIO_REQUEST_URI);
        toAddress.ifPresent(h -> {
            final AddressParametersHeader addressHeader = (AddressParametersHeader) h;
            this.callLog.setTo(parseCallerId(addressHeader.getAddress()));
        });
        this.callLog.setFlag(CallLog.TRUNKING_TERMINATING_FLAG);

        ((SipURI) linked.build().getRequestUri()).setParameter().to
        requestURI.setUserParam("phone");
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

    @Override
    private void doAck(final SipRequest request) throws ServletException, IOException {
        final B2buaHelper b2buaHelper = new TwilioB2buaHelper(request);
        final SipSession linkedSession = b2buaHelper.getLinkedSession(request.getSession());
        if (linkedSession != null && linkedSession.isValid()) {
            final SipRequest outstandingAck =
                    (SipRequest) linkedSession.getAttribute(SessionAttributes.OUTSTANDING_ACK);
            if (outstandingAck != null) {
                if(request.getContent() != null && request.getContentType() != null) {
                    outstandingAck.setContentLength(request.getContentLength());
                    outstandingAck.setContent(request.getContent(), request.getContentType());
                }
                sendSipMessage(outstandingAck);
            }
        }
    }

    private void doBye(final SipRequest request) throws ServletException, IOException {
        // TODO: should handle BYE in transfer case!!!!

        final TwilioB2buaHelper b2buaHelper = new TwilioB2buaHelper(request);

        // This terminates calls
        if (updateStatus(request, Status.COMPLETED)) {
            // B2BUA BYE
            final SipRequest linkedRequest = b2buaHelper.createBye();
            if (linkedRequest != null) {
                copyByeHeaders(request, linkedRequest);
                sendSipMessage(linkedRequest);
            } else {
                logInfo(request, "Can't send BYE to already terminated linked session.");
                sendSipMessage(request.createResponse(200));
            }

            releaseMediaSession(request.getApplicationSession());
        } else {
            logInfo(request, "Received BYE at invalid status. Responding with 200 OK "
                    + "but won't forward BYE to the other side.");
            sendSipMessage(request.createResponse(200));

            // clean up SAS
            request.getApplicationSession().invalidate();
        }
    }

    private void doInfo(final SipRequest request) {
        final TwilioB2buaHelper b2buaHelper = new TwilioB2buaHelper(request);
        final SipSession linkedSession = b2buaHelper.getLinkedSession(request.getSession());
        if (linkedSession != null) {
            try {
                final SipRequest linkedRequest = linkedSession.createRequest("INFO");
                linkedRequest.setAttribute(ORIGINAL_REQ, request);
                linkedRequest.setContent(request.getContent(), request.getContentType());
                copyHeaders(request, linkedRequest, ImmutableList.of("Info-Package", "Content-Disposition"));
                sendSipMessage(linkedRequest);
            } catch (final Exception e) {
                logInfo(linkedSession.getApplicationSession(), "Unable to create INFO message: " + e.getMessage());
                sendSipMessage(request.createResponse(200));
            }
        }
    }

    private void doCancel(final SipRequest request) throws ServletException, IOException {
        if (updateStatus(request, Status.NO_ANSWER)) {
            // send b2buaed CANCEL to the other leg
            final TwilioB2buaHelper b2buaHelper = new TwilioB2buaHelper(request);
            final SipSession linkedSession = b2buaHelper.getLinkedSession(request.getSession());
            final SipRequest linkedRequest = b2buaHelper.createCancel(linkedSession);
            if (linkedRequest != null) {
                sendSipMessage(linkedRequest);
                // Set attr_cancelling to know this session is canceled when receiving response.
                linkedSession.setAttribute(SessionAttributes.CANCELLING, true);
            }

            releaseMediaSession(request.getApplicationSession());

            // Note that container sends "200 canceling" for the CANCEL request
            // and 487 for the INVITE to the original sender
        } else {
            logInfo(request, "Received CANCEL at invalid status so dropping it.");
        }
    }

    private void doRefer(final SipRequest request) throws ServletException, IOException {
        final SipApplicationSession sas = request.getApplicationSession(false);
        if (sas != null) {
            final TwilioB2buaHelper b2buaHelper = new TwilioB2buaHelper(request);
            final SipSession linkedSession = b2buaHelper.getLinkedSession(request.getSession());
            final String referTo = request.getHeader("Refer-To");
            final String callSid = getCallSid(request);
            logDebug(request, "Received refer target: " + referTo);

            // reply to transferer with 202
            final SipResponse response = request.createResponse(202);
            sendSipMessage(response);

            // create and send invite with no SDP to transfer target
            final SipRequest createdRequest =
                    this.sipFactory.createRequest(sas, "INVITE", linkedSession.getRemoteParty(),
                            this.sipFactory.createAddress(referTo));
            // createdRequest.pushRoute(getNextHopURI());
            sendSipMessage(createdRequest);

            // store transfer state
            final CallLog transferCallLog = new CallLog(callSid);
            transferCallLog.setStatus(Status.UNDIALED);
            sas.setAttribute(SessionAttributes.TRANSFER_TRANSFERER_SESSION, request.getSession());
            sas.setAttribute(SessionAttributes.TRANSFER_TRANSFEREE_SESSION, linkedSession);
            sas.setAttribute(SessionAttributes.TRANSFER_TARGET_SESSION, createdRequest.getSession());
            sas.setAttribute(SessionAttributes.TRANSFER_CALLLOG, transferCallLog);
        }
    }

    /**
     * B2BUA success response to the other side
     */
    private void doSuccessResponse(final SipResponse response) throws ServletException, IOException {
        final String method = response.getMethod();
        final SipApplicationSession sas = response.getApplicationSession(false);
        if (sas != null) {
            if (method.equals("INVITE")) {
                boolean deferAck = false;
                if (sas.getAttribute(SessionAttributes.TRANSFER_CALLLOG) == null) {
                    if (updateStatus(response, Status.ANSWERED)) {
                        checkVerificationCall(response);
                        // If this is trunking-terminating, try to update ProviderSid
                        final CallLog callLog = (CallLog) sas.getAttribute(SessionAttributes.CALLLOG);
                        if (callLog.isFlagEnabled(CallLog.TRUNKING_TERMINATING_FLAG)) {
                            final String providerSid = response.getHeader(TwilioHeaders.X_TWILIO_PROVIDER_SID);
                            if (providerSid != null && !providerSid.isEmpty()) {
                                callLog.setProviderSid(providerSid);
                            }
                        }

                        final SipResponse linkedResponse = createLinkedResponseToInvite(response);
                        if (linkedResponse != null) {
                            final MediaSession mediaSession = getMediaSession(sas);
                            if (mediaSession != null) {
                                final MediaSession.AnswerBuilder answer = mediaSession.withAnswer(response.getContent());
                                Futures.addCallback(answer.send(), new FutureCallback<SdpResponse>() {
                                    @Override
                                    public void onSuccess(final SdpResponse result) {
                                        try {
                                            linkedResponse.setContent(result.getSdp(), APPLICATION_SDP);
                                        } catch (final UnsupportedEncodingException e) {
                                            logWarn(linkedResponse, "Failed to update SDP", e);
                                        }
                                        sendSipMessage(linkedResponse);
                                    }

                                    @Override
                                    public void onFailure(final Throwable t) {
                                        logWarn(response, "Media server error response. Terminating session", t);
                                        sendSipMessage(response.getSession().createRequest("BYE"));
                                        rejectCall(linkedResponse.getRequest(), response.getRequest(), 500,
                                                "Media Server Error");
                                    }
                                });
                            } else {
                                sendSipMessage(linkedResponse);
                            }
                            deferAck = true;
                        }
                    } else {
                        logInfo(response, "Received success response at invalid status. Won't forward it to the other leg.");
                    }
                } else {
                    // Transfer case
                    final CallLog transferCallLog = (CallLog) sas.getAttribute(SessionAttributes.TRANSFER_CALLLOG);
                    if (transferCallLog.getStatus() == Status.RINGING) {
                        // When receiving 200 from transfer-target, send re-INVITE to transferee
                        final SipSession transfereeSession = (SipSession) sas.getAttribute(SessionAttributes.TRANSFER_TRANSFEREE_SESSION);
                        final SipRequest reInviteRequest = transfereeSession.createRequest("INVITE");
                        sendSipMessage(reInviteRequest);
                        transferCallLog.setStatus(Status.ANSWERED);
                    } else if (transferCallLog.getStatus() == Status.ANSWERED) {
                        // When receiving 200 from transferee for re-INVITE
                        // TODO: send ACK to the target
                        sas.removeAttribute(SessionAttributes.TRANSFER_CALLLOG);
                    }
                }

                if (deferAck) {
                    // Wait to send ACK until we get ACK from linked session to be able to copy SDP if any
                    response.getSession().setAttribute(SessionAttributes.OUTSTANDING_ACK, response.createAck());
                } else {
                    // Send ACK now toward the sender of 200
                    sendSipMessage(response.createAck());
                }

                if (response.getSession().getAttribute(SessionAttributes.CANCELLING) != null) {
                    // Response is for a cancelled INVITE, we must send BYE
                    logInfo(response, "Received success response for cancelled INVITE. Will send BYE.");
                    final SipRequest byeRequest = response.getSession().createRequest("BYE");
                    sendSipMessage(byeRequest);
                }
            } else if (method.equals("BYE")) {
                logInfo(response, "Received final response. Call is terminated. AppSessionId: " + sas.getId());
                final SipResponse linkedResponse = createLinkedResponseToBye(response);
                if (linkedResponse != null) {
                    copyByeHeaders(response, linkedResponse);
                    sendSipMessage(linkedResponse);
                }
                sas.invalidate();
            } else if (method.equals("INFO")) {
                final SipRequest originalRequest = (SipRequest)response.getRequest().getAttribute(ORIGINAL_REQ);
                final SipResponse r = originalRequest.createResponse(200);
                sendSipMessage(r);
            }
        }
    }

    private void doErrorResponse(final SipResponse response) throws ServletException, IOException {
        final SipApplicationSession sas = response.getApplicationSession(false);
        if (sas != null) {
            final String method = response.getMethod();
            final SipSession session = response.getSession();
            logInfo(response, "Received final response. Call is terminated. AppSessionId: " + sas.getId());

            if (method.equals("INVITE")) {
                if (sas.getAttribute(SessionAttributes.TRANSFER_CALLLOG) == null) {

                    // If the response is for the canceled INVITE, we don't need to do anything
                    // because container already sent 487 to the sender of cancel
                    if (session.getAttribute(SessionAttributes.CANCELLING) == null) {
                        // Regular case
                        Status callLogStatus = Status.FAIL;
                        if (response.getStatus() == SipResponse.SC_BUSY_HERE) {
                            callLogStatus = Status.BUSY;
                        }

                        if (updateStatus(response, callLogStatus)) {
                            final SipResponse linkedResponse = createLinkedResponseToInvite(response);
                            if (linkedResponse != null) {
                                sendSipMessage(linkedResponse);
                            }

                            releaseMediaSession(sas);
                        } else {
                            logInfo(response, "Received error response at invalid status so dropping it.");
                        }
                    }
                } else {
                    // Transfer case
                    final CallLog transferCallLog = (CallLog) sas.getAttribute(SessionAttributes.TRANSFER_CALLLOG);
                    if (transferCallLog.getStatus() == Status.UNDIALED || transferCallLog.getStatus() == Status.RINGING) {
                        // When receiving error from transfer-target,
                        // TODO: send NOTIFY to transferer
                    }
                }

                // This is the final response to INVITE so let's clean up SAS. Note that even in the
                // CANCEL case with many derived sessions, this will clean up everything.
                sas.invalidate();
            } else if (method.equals("BYE")) {
                final SipResponse linkedResponse = createLinkedResponseToBye(response);
                if (linkedResponse != null) {
                    copyByeHeaders(response, linkedResponse);
                    sendSipMessage(linkedResponse);
                }
                sas.invalidate();
            }
            // Note that ACK for error response is sent by container.
        }
    }

    private void doProvisionalResponse(final SipResponse response) throws ServletException, IOException {
        final SipApplicationSession sas = response.getApplicationSession(false);
        if (sas != null) {
            final int statusCode = response.getStatus();
            final String method = response.getMethod();
            if (method.equals("INVITE") && statusCode >= 180 && statusCode <= 183) {
                if (sas.getAttribute(SessionAttributes.TRANSFER_CALLLOG) == null) {
                    // Regular case
                    if (updateStatus(response, Status.RINGING)) {
                        checkVerificationCall(response);
                        final SipResponse linkedResponse = createLinkedResponseToInvite(response);
                        if (linkedResponse != null) {
                            final MediaSession mediaSession = getMediaSession(sas);
                            if (mediaSession != null && APPLICATION_SDP.equals(response.getContentType())) {
                                final MediaSession.AnswerBuilder answer = mediaSession.withAnswer(response.getContent())
                                        .withProvisional(true);
                                Futures.addCallback(answer.send(), new FutureCallback<SdpResponse>() {
                                    @Override
                                    public void onSuccess(final SdpResponse result) {
                                        try {
                                            linkedResponse.setContent(result.getSdp(), APPLICATION_SDP);
                                        } catch (final UnsupportedEncodingException e) {
                                            logWarn(response, "Failed to update SDP", e);
                                        }
                                        sendSipMessage(linkedResponse);
                                    }

                                    @Override
                                    public void onFailure(final Throwable t) {
                                        logWarn(response, "Media server error on provisional response (ignored)", t);
                                    }
                                });
                            } else {
                                sendSipMessage(linkedResponse);
                            }
                        }
                    } else {
                        logInfo(response, "Received provisional response at invalid status so dropping it.");
                    }
                } else {
                    // Transfer case
                    final CallLog transferCallLog = (CallLog) sas.getAttribute(SessionAttributes.TRANSFER_CALLLOG);
                    if (transferCallLog.getStatus() == Status.UNDIALED) {
                        // TODO: Send notify to the tranferer
                        transferCallLog.setStatus(Status.RINGING);
                    }
                }
            }
        }
    }

    /**
     * This method creates a b2buaed response from a response to INVITE.
     *
     * @param response
     * @throws IOException
     */
    private SipResponse createLinkedResponseToInvite(final SipResponse response) throws IOException {
        final SipSession session = response.getSession(false);
        final TwilioB2buaHelper b2buaHelper = new TwilioB2buaHelper(response.getRequest());
        final SipSession linkedSession = b2buaHelper.getLinkedSession(session);
        final SipResponse responseOther =
                b2buaHelper.createResponseToOriginalRequest(linkedSession, response.getStatus(),
                        response.getReasonPhrase());
        if (responseOther != null && response.getContentType() != null) {
            responseOther.setContent(response.getContent(), response.getContentType());
        }
        b2buaHelper.updateSession(session);
        return responseOther;
    }

    private SipResponse createLinkedResponseToBye(final SipResponse response) {
        final TwilioB2buaHelper b2buaHelper = new TwilioB2buaHelper(response.getRequest());
        final SipResponse linkedResponse = b2buaHelper.createLinkedResponseToBye(response);
        return linkedResponse;
    }

    private boolean updateStatus(final SipMessage message, final CallLog.Status status) {
        final SipApplicationSession as = message.getApplicationSession(false);
        return updateStatus(as, message, status);
    }

    private boolean updateStatus(final SipApplicationSession as, final CallLog.Status status) {
        return updateStatus(as, null, status);
    }

    /**
     * This method updates the call status. Also call terminateCall() if status is final.
     * Returns true if the SIP message needs to be forwarded to the other leg.
     *
     * @param as
     * @param message
     * @param status
     * @return
     */
    private boolean updateStatus(final SipApplicationSession as, final SipMessage message, final CallLog.Status status) {
        boolean needForward = false;
        if (as != null) {
            final CallLog callLog = (CallLog) as.getAttribute(SessionAttributes.CALLLOG);
            if (callLog == null) {
                logWarn(message, "No CallLog entry associated with message. Probably an internal service issue. Please file a bug to Voicecon.");
            }

            // update only current status is not final
            if (!callLog.isFinalStatus()) {
                needForward = true;
                if (callLog.getStatus() != status) {
                    final String msg = "Changed status : " + callLog.getStatus() + " -> " + status;
                    if (message != null) {
                        logInfo(message, msg);
                    } else {
                        logInfo(as, msg);
                    }
                    // If state is answered, update start time so we can calculate call duration
                    // accurately. Also set hasEstablished flag for billing later.
                    if (status == Status.ANSWERED) {
                        callLog.setStartTime(LocalDateTime.now());
                        callLog.established();
                    }

                    callLog.setDateUpdated(DateTime.now());
                    callLog.setStatus(status);

                    // If new status is final, terminate call
                    if (callLog.isFinalStatus()) {
                        terminateCall(as, callLog);
                    }
                }
            }
        }
        return needForward;
    }

    /**
     * This method terminates the call by sending postflight and billing request.
     *
     * @param sas
     * @param callLog
     */
    private void terminateCall(final SipApplicationSession sas, final CallLog callLog) {
        callLog.setEndTime(DateTime.now());
        try {
            callLog.calculateAndSetDuration();
            logInfo(sas, callLog, "Call terminated with status " + callLog.getStatus() + " Duration of call (sec) " + callLog.getDuration());
            callLog.callLogHasBeenReported();
            callPostflight(sas, callLog);

            // send billing event only if call has been established and call duration is not 0.
            // so we bill even when the final status is not completed, but has established before.
            if (callLog.hasEstablished() && callLog.getDuration() > 0 && !callLog.isVerificationCall()) {
                callBilling(sas, callLog);
            } else {
                logInfo(sas, callLog, "Not sending billing event because call status has not been established or duration is 0.");
            }
        } catch (final IllegalArgumentException e) {
            logWarn(sas, callLog, "Failed to calculate call duration (sec): " + callLog);
        }
    }

    /**
     * This method sends a request to the postflight-voice-api to write the call log. Invoked when
     * the call state reaches to the final state.
     *
     * @param sas
     * @param callLog
     */
    private void callPostflight(final SipApplicationSession sas, final CallLog callLog) {
        if (this.postflight == null) {
            logWarn(sas, callLog, "Not sending postflight request because it is disabled");
            return;
        }

        final String sasId = sas.getId();
        final Call call = callLog.getCall();
        final ListenableFuture<Void> resp = this.postflight.writeFinalCall(callLog.getAccountSid(), call);
        // Note that application session might be already invalidated when callback is invoked.
        // So don't try to access application session.
        Futures.addCallback(resp, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                logger.info(buildLogMessage(sasId, callLog, "Successfully written to postflight.\n"
                        + PostflightUtils.convertCallToJson(call)));
            }

            @Override
            public void onFailure(final Throwable t) {
                logger.warn(buildLogMessage(sasId, callLog, "Received error from postflight. Error message: "
                        + t.getMessage() + "\n"
                        + PostflightUtils.generatePostflightUrl(callLog.getAccountSid(), callLog.getSid()) + "\n"
                        + PostflightUtils.convertCallToJson(call)));
            }
        });
    }

    /**
     * This method sends a billing event to AWS SQS billing queue. Invoked at the end of each call.
     *
     * @param sas
     * @param callLog
     */
    private void callBilling(final SipApplicationSession sas, final CallLog callLog) {
        if (this.billing == null) {
            logWarn(sas, callLog, "Not sending billing event because it is disabled");
            return;
        }

        final String sasId = sas.getId();
        final List<VoiceBillingEvent> billingEvents = BillingUtils.convertToBillingEvents(callLog);
        for (final VoiceBillingEvent billingEvent : billingEvents) {
            final ListenableFuture<String> future = this.billing.sendMessageAsync(billingEvent);
            Futures.addCallback(future, new FutureCallback<String>() {
                @Override
                public void onSuccess(final String result) {
                    logger.info(buildLogMessage(sasId, callLog, "Successfully sent billing event. MessageId: "
                            + result + "\n"
                            + billingEvent.toXml()));
                }

                @Override
                public void onFailure(final Throwable t) {
                    logger.warn(buildLogMessage(sasId, callLog, "Received error from billing. Error message: "
                            + t.getMessage() + "\n"
                            + billingEvent.toXml()));
                }
            });
        }
    }

    /**
     * This method contains common tasks before sending SIP messages. Used for both request and
     * response.
     *
     * @param message
     */
    private void sendSipMessage(final SipMessage message) {
        try {
            message.setHeader(callLog.getCallSidHeader());
            message.send();
        } catch (final IOException e) {
            logWarn(message, "Failed to send SIP message. Error message: " + e);
        }
    }

    /**
     * Helper to read callsid from application session. Return null if there is no callsid in as.
     *
     * @param message
     * @return
     */
    private String getCallSid(final SipMessage message) {
        final SipApplicationSession as = message.getApplicationSession(false);
        if (as != null) {
            return (String) as.getAttribute(SessionAttributes.CALLSID);
        }
        return null;
    }

    /**
     * Helper to read a SIP header in the request and return as SipURI format
     *
     * @param request
     * @param headerName
     * @return
     * @throws SipHeaderParseException
     */
    private SipURI getSipURIFromHeader(final SipRequest request, final String headerName)
            throws SipHeaderParseException {
        try {
            final Address address = request.getAddressHeader(headerName);
            if (address != null) {
                return (SipURI) address.getURI();
            } else {
                logger.info("Header " + headerName + " is null.");
                throw new ServletParseException();
            }
        } catch (final ServletParseException e) {
            throw new SipHeaderParseException(e, headerName);
        } catch (final ClassCastException e) {
            throw new SipHeaderParseException(e, headerName);
        }
    }

    /**
     * Helper to extract caller information from SIP address. If the user part of the address is
     * E.164, return only the user part. Otherwise, return the SIP URI.
     *
     * @param address
     * @return
     */
    private String parseCallerId(final Address address) {
        final SipURI uri = (SipURI) address.getURI();
        if (SipUtil.isValidE164Number(uri.getUser())) {
            return uri.getUser();
        } else {
            return parseSipUri(uri);
        }
    }

    /**
     * Helper to extract SIP URI without any parameters.
     *
     * @param uri
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
     * @param sipMessage
     * @return
     * @throws IllegalArgumentException
     */
    private EdgeType parseEdgeType(final SipMessage sipMessage) throws IllegalArgumentException {
        final String user = sipMessage.getFromHeader().getParameter("user").toString();
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
        copyHeaders(from, to, this.config.getCopyByeHeaders());
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
                        getCallSid(message), message.getCallIDHeader().getCallId(), str);
    }

    private String buildLogMessage(final String sasId, final CallLog callLog, final String msg) {
        return String.format("[%s,%s] %s", sasId, callLog.getSid(), msg);
    }

    private void logDebug(final SipMessage message, final String str) {
        if (logger.isDebugEnabled()) {
            logger.debug(buildLogMessage(message, str));
        }
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

    private void logWarn(final SipMessage message, final String str) {
        logger.warn(buildLogMessage(message, str));
    }

    private void logWarn(final SipMessage message, final String str, final Throwable t) {
        logger.warn(buildLogMessage(message, str), t);
    }

    private void logError(final SipMessage message, final String str) {
        logger.error(buildLogMessage(message, str));
    }

    private void logError(final SipMessage message, final String str, final Throwable t) {
        logger.error(buildLogMessage(message, str), t);
    }

    private void checkVerificationCall(final SipMessage msg) {
        if ( null != msg.getHeader(TwilioHeaders.X_TWILIO_VERIFICATION_CALL) ) {
            callLog.setVerificationCall(true);
            callLog.setFlag(CallLog.TRUNKING_VERIFICATION_CALL_FLAG);
            callLog.setPrice(0.0);
        }
    }
}
