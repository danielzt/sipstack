package io.sipstack.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.sipstack.netty.codec.sip.Transport;

import java.time.Duration;

/**
 * Keep-alive traffic is very important for flow management.
 * The basic configuration is just whether or not keep-alive
 * traffic should be on/off but the flows within sipstack.io
 * is capable of issuing keep-alive traffic themselves, even
 * when the stack may not have been the one initiating the flow
 * to begin with. Also, depending on if this is UDP, TCP you may
 * want to use different type of keep-alive traffic.
 *
 * @author jonas@jonasborjesson.com
 */
public class KeepAliveConfiguration {

    @JsonProperty
    private KEEP_ALIVE_MODE mode = KEEP_ALIVE_MODE.ACTIVE;

    @JsonProperty
    private Duration idleTimeout = Duration.ofSeconds(120);

    @JsonProperty
    private int maxFailed = 3;

    @JsonProperty
    private Duration interval = Duration.ofSeconds(5);

    @JsonProperty
    private boolean enforcePong = false;

    @JsonProperty
    private KeepAliveMethodConfiguration udp = new KeepAliveMethodConfiguration();

    @JsonProperty
    private KeepAliveMethodConfiguration tcp = new KeepAliveMethodConfiguration();

    @JsonProperty
    private KeepAliveMethodConfiguration ws = new KeepAliveMethodConfiguration();

    public KeepAliveMethodConfiguration getKeepAliveMethodConfiguration(final Transport transport) {
        switch(transport) {
            case udp:
            case sctp:
                return udp;
            case tcp:
            case tls:
                return tcp;
            case ws:
            case wss:
                return ws;
            default:
                throw new IllegalArgumentException("unknown transport");
        }
    }


    public KeepAliveMethodConfiguration getUdpKeepAliveMethodConfiguratioh() {
        return udp;
    }

    public KeepAliveMethodConfiguration getTcpKeepAliveMethodConfiguratioh() {
        return tcp;
    }

    public KeepAliveMethodConfiguration getWsKeepAliveMethodConfiguratioh() {
        return ws;
    }

    /**
     * See javadoc on {@link KEEP_ALIVE_MODE}.
     *
     * @return
     */
    public KEEP_ALIVE_MODE getMode() {
        return mode;
    }

    public void setMode(final KEEP_ALIVE_MODE mode) {
        this.mode = mode == null ? KEEP_ALIVE_MODE.NONE : mode;
    }

    /**
     * If we are in {@link KEEP_ALIVE_MODE#ACTIVE} mode then we will actively monitor the traffic
     * and if this timer fires, we will start issuing keep-alive traffic.
     *
     * For the other modes, this setting has no meaning.
     * @return
     */
    public Duration getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(final Duration idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    /**
     * If we are in {@link KEEP_ALIVE_MODE#ACTIVE} mode and the idle timeout has
     * fired we will start issuing keep-alive traffic.
     *
     * and if this timer fires, we will start issuing keep-alive traffic.
     *
     * For the other modes, this setting has no meaning.
     * @return
     */
    public int getMaxFailed() {
        return maxFailed;
    }

    public void setMaxFailed(final int maxFailed) {
        this.maxFailed = maxFailed;
    }

    public Duration getInterval() {
        return interval;
    }

    public void setInterval(final Duration interval) {
        this.interval = interval;
    }

    /**
     * If we are sending out PING traffic, because we are in ACTIVE mode and the idle timer
     * has fired, then this setting controls whether or not we are forcing the remote
     * UA to respond with a pong. If we do not get a pong, then we consider that a
     * failed attempt, which counts towards the "maxFailed" attempt setting.
     */
    public boolean getEnforcePong() {
        return enforcePong;
    }

    public void setEnforcePong(final boolean value) {
        enforcePong = value;
    }

    /**
     * If your SIP network has a lot of internal nodes and you own and control
     * this network then there is no reason for keep-alive traffic so turn it off.
     * You really only need to turn it on when your SIP server is at the edge of
     * your network.
     */
    public static enum KEEP_ALIVE_MODE {

        /**
         * Completely turn off any keep-alive traffic. If a client
         * would send any type of keep-alive traffic to it (such as double CRLF)
         * it will simply be consumed and ignored.
         */
        NONE,

        /**
         * According to RFC5626 only the entity that initiates the flow
         * is responsible for issuing keep-alive traffic. However, because
         * of e.g. backgrounded iOS apps, those apps cannot issue any
         * application level ping anymore and therefore the server has
         * to do it. However, in passive mode, the flow will not do this but
         * if you set it to ACTIVE, the flow will be willingly to issue
         * application level ping traffic even if it shouldn't according to
         * RFC5626.
         */
        PASSIVE,

        /**
         * See comment on passive
         */
        ACTIVE;
    }


}
