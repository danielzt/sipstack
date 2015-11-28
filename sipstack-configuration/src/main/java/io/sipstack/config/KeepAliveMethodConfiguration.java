package io.sipstack.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

import static io.sipstack.config.KeepAliveMethodConfiguration.PING_METHOD.DOUBLE_CRLF;
import static io.sipstack.config.KeepAliveMethodConfiguration.PING_METHOD.SIP_OPTIONS;
import static io.sipstack.config.KeepAliveMethodConfiguration.PING_METHOD.STUN;

/**
 * If we turn keep-alive on then we also need to configure
 * what type of keep-alive traffic we are going to accept
 * and what we will actually be generating ourselves.
 * This class keeps track of that configuration.
 *
 * @author jonas@jonasborjesson.com
 */
public class KeepAliveMethodConfiguration {

    /**
     * The is the one we will be using if we are configured
     * to actively send pings. We will ONLY always just
     * actively use one method of ping traffic. However,
     * we may accept many incoming types of ping traffic.
     */
    @JsonProperty
    private PING_METHOD use = SIP_OPTIONS;

    /**
     * If we use SIP Options as the method for our active ping
     * then we need to setup some additional things.
     */
    @JsonProperty
    private SipOptionsPingConfiguration sipOptions = new SipOptionsPingConfiguration();

    /**
     * We want jackson to go through the methods instead of
     * messing with the property directly. This so that we
     * can extract out the various values and store them
     * directly as booleans.
     */
    @JsonIgnore
    private boolean acceptSipOptions = false;

    @JsonIgnore
    private boolean acceptDoubleCRLF = false;

    @JsonIgnore
    private boolean acceptStun = false;

    public PING_METHOD getActiveMethod() {
        return use;
    }

    /**
     * Convenience method for checking if the active
     * ping method to use is SIP Options.
     *
     * @return
     */
    public boolean useSipOptions() {
        return getActiveMethod() == SIP_OPTIONS;
    }

    public boolean useDblCrlf() {
        return getActiveMethod() == DOUBLE_CRLF;
    }

    public boolean useStun() {
        return getActiveMethod() == STUN;
    }

    public boolean acceptStun() {
        return acceptStun;
    }

    public boolean acceptSipOptions() {
        return acceptSipOptions;
    }

    public boolean acceptDoubleCRLF() {
        return acceptDoubleCRLF;
    }

    public SipOptionsPingConfiguration getSipOptionsConfiguration() {
        return sipOptions;
    }

    public void setActiveMethod(final PING_METHOD method) {
        if (method == null) {
            throw new IllegalArgumentException("Ping method cannot be null");
        }
        use = method;
    }

    @JsonProperty("accept")
    public List<PING_METHOD> getAcceptedMethods() {
        final List<PING_METHOD> list = new ArrayList<>(3);
        if (acceptSipOptions) {
            list.add(SIP_OPTIONS);
        }
        if (acceptDoubleCRLF) {
            list.add(PING_METHOD.DOUBLE_CRLF);
        }
        if (acceptStun) {
            list.add(PING_METHOD.STUN);
        }
        return list;
    }

    @JsonProperty("accept")
    public void setAcceptedMethods(final List<PING_METHOD> methods) {
        for (final PING_METHOD method : methods) {
            switch (method) {
                case SIP_OPTIONS:
                    acceptSipOptions = true;
                break;
                case DOUBLE_CRLF:
                    acceptDoubleCRLF = true;
                    break;
                case STUN:
                    acceptStun = true;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown PING method. Did you add to enum and forgot to update here?");

            }
        }
    }

    /**
     * If we configure the flow to actually accept and/or generate
     * keep-alive (ping) traffic then these are the different
     * options we have. Note, we may actually allow them all
     * at once.
     */
    public static enum PING_METHOD {
        SIP_OPTIONS, DOUBLE_CRLF, STUN;
    }
}
