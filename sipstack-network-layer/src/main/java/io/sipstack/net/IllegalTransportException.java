package io.sipstack.net;

/**
 * Indicates that the {@link Transport} cannot be used in this context.
 * 
 * @author jonas@jonasborjesson.com
 */
public class IllegalTransportException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    public IllegalTransportException() {
        // left empty intentionally
    }

    public IllegalTransportException(final String s) {
        super(s);
    }

}
