/**
 * 
 */
package io.sipstack.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;

/**
 * All timer values as defined by RFC3261.
 * 
 * @author jonas@jonasborjesson.com
 */
public final class TimersConfiguration {
    
    /**
     * T1       500ms default    Section 17.1.1.1     RTT Estimate
     */
    @JsonProperty
    private Duration t1 = Duration.ofMillis(500);

    /**
     * T2       4s               Section 17.1.2.2     The maximum retransmit
     *                                                interval for non-INVITE
     *                                                requests and INVITE
     *                                                responses
     */
    @JsonProperty
    private Duration t2 = Duration.ofSeconds(4);

    /**
     * T4       5s               Section 17.1.2.2     Maximum duration a
     *                                                message will
     *                                                remain in the network
     */
    @JsonProperty
    private Duration t4 = Duration.ofSeconds(5);
    
    /**
     * Timer A  initially T1     Section 17.1.1.2     INVITE request retransmit
     *                                                interval, for UDP only
     * @return
     */
    @JsonIgnore
    private Duration timerA;
    
    /**
     * 
     * Timer B  64*T1            Section 17.1.1.2     INVITE transaction
     *                                                timeout timer
     */
    @JsonIgnore
    private Duration timerB;
    
    /**
     * Timer C  > 3min           Section 16.6         proxy INVITE transaction
     *                            bullet 11            timeout
     */
    @JsonIgnore
    private final Duration timerC = Duration.ofMinutes(3);
    
    /**
     * Timer D  > 32s for UDP    Section 17.1.1.2     Wait time for response
     *            0s for TCP/SCTP                       retransmits
     */
    @JsonIgnore
    private final Duration timerD = Duration.ofSeconds(32);
    
    /**
     * Timer E  initially T1     Section 17.1.2.2     non-INVITE request
     *                                                retransmit interval,
     *                                                UDP only
     */
    @JsonIgnore
    private Duration timerE;
    
    /**
     * Timer F  64*T1            Section 17.1.2.2     non-INVITE transaction
     *                                                timeout timer
     */
    @JsonIgnore
    private Duration timerF;
    
    /**
     * Timer G  initially T1     Section 17.2.1       INVITE response
     *                                                retransmit interval
     */
    @JsonIgnore
    private Duration timerG;
    
    /**
     * Timer H  64*T1            Section 17.2.1       Wait time for
     *                                                ACK receipt
     */
    @JsonIgnore
    private Duration timerH;
    
    /**
     * Timer I  T4 for UDP       Section 17.2.1       Wait time for
     *          0s for TCP/SCTP                       ACK retransmits
     */
    @JsonIgnore
    private Duration timerI;
    
    /**
     * Timer J  64*T1 for UDP    Section 17.2.2       Wait time for
     *          0s for TCP/SCTP                       non-INVITE request
     *                                                retransmits
     */
    @JsonIgnore
    private Duration timerJ;
    
    /**
     * Timer K  T4 for UDP       Section 17.1.2.2     Wait time for
     *          0s for TCP/SCTP                       response retransmits
     */
    @JsonIgnore
    private Duration timerK;

    /**
     * Timer L  64*T1            Section 7.1 (rfc6026)
     */
    @JsonIgnore
    private Duration timerL;

    public TimersConfiguration() {
        init();
    }
    
    /**
     * Called as soon as the values of T1, T2, T4 changes since
     * the timers below are based on the values of those base timers.
     */
    private void init() {
        this.timerA = Duration.ofMillis(this.t1.toMillis());
        this.timerB = Duration.ofMillis(64 * this.t1.toMillis());
        this.timerE = Duration.ofMillis(this.t1.toMillis());
        this.timerF = Duration.ofMillis(64 * this.t1.toMillis());
        this.timerG = Duration.ofMillis(this.t1.toMillis());
        this.timerH = Duration.ofMillis(64 * this.t1.toMillis());
        this.timerI = Duration.ofMillis(this.t4.toMillis());

        this.timerJ = Duration.ofMillis(64 * this.t1.toMillis());
        // this.timerJ = Duration.ofMillis(1 * this.t1.toMillis());

        this.timerK = Duration.ofMillis(this.t4.toMillis());

        this.timerL = Duration.ofMillis(64 * this.t1.toMillis());
        // this.timerL = Duration.ofMillis(1 * this.t1.toMillis());
    }

    public Duration getT1() {
        return this.t1;
    }

    public Duration getT2() {
        return this.t2;
    }

    public Duration getT4() {
        return this.t4;
    }

    public void setT1(final Duration t1) {
        this.t1 = t1;
        init();
    }

    public void setT2(final Duration t2) {
        this.t2 = t2;
        init();
    }

    public void setT4(final Duration t4) {
        this.t4 = t4;
        init();
    }

    public Duration getTimerA() {
        return this.timerA;
    }

    public Duration getTimerB() {
        return this.timerB;
    }

    public Duration getTimerC() {
        return this.timerC;
    }

    public Duration getTimerD() {
        return this.timerD;
    }

    public Duration getTimerE() {
        return this.timerE;
    }

    public Duration getTimerF() {
        return this.timerF;
    }

    public Duration getTimerG() {
        return this.timerG;
    }

    public Duration getTimerH() {
        return this.timerH;
    }

    public Duration getTimerI() {
        return this.timerI;
    }

    public Duration getTimerJ() {
        return this.timerJ;
    }

    public Duration getTimerK() {
        return this.timerK;
    }

    public Duration getTimerL() {
        return this.timerL;
    }


}
