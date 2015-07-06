package io.sipstack.net;

/**
 * @author jonas@jonasborjesson.com
 */
public interface NetworkLayer {

    /**
     * Start the {@link NetworkLayer}, which will call {@link NetworkInterface#up()} on all
     * the configured network interfaces.
     */
    void start();

    /**
     * Hang on this network layer until all interfaces have been shutdown and as such
     * this network is stopped.
     *
     * @throws InterruptedException
     */
    void sync() throws InterruptedException;
}
