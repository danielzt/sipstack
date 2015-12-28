package io.sipstack.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.pkts.packet.sip.address.Address;
import io.pkts.packet.sip.address.SipURI;
import io.pkts.packet.sip.address.impl.SipURIImpl;

import java.util.Optional;

/**
 * If we are configured to send SIP Options as a ping then
 * we must also know e.g. what To- and From-headers we should
 * use. The target (request-uri) of the OPTIONS request will be
 * wherever the flow is pointing to but you can configure the
 * user portion of the request-uri.
 *
 * @author jonas@jonasborjesson.com
 */
public class SipOptionsPingConfiguration {

    // note, if you change these then please update documentation
    private final static String DEFAULT_TARGET_USER = "ping";
    private final static String DEFAULT_FROM_USER = "ping";
    private final static String DEFAULT_TO_USER = "ping";

    @JsonProperty
    private String targetUser = DEFAULT_TARGET_USER;

    @JsonProperty
    private String fromUser = DEFAULT_FROM_USER;

    @JsonProperty
    private String fromHost;

    @JsonProperty
    private String toUser = DEFAULT_TO_USER;

    @JsonProperty
    private String toHost;

    public String getTargetUser() {
        return targetUser;
    }

    public void setTargetUser(final String user) {
        targetUser = user == null || user.isEmpty() ? DEFAULT_TARGET_USER : user;
    }

    public String getToUser() {
        return toUser;
    }

    public void setToUser(final String user) {
        toUser = user == null || user.isEmpty() ? DEFAULT_TO_USER : user;
    }

    public Optional<String> getToHost() {
        return Optional.ofNullable(toHost);
    }

    public String getFromUser() {
        return fromUser;
    }

    public void setFromUser(final String user) {
        fromUser = user == null || user.isEmpty() ? DEFAULT_FROM_USER : user;
    }

    public Optional<String> getFromHost() {
        return Optional.ofNullable(fromHost);
    }

}
