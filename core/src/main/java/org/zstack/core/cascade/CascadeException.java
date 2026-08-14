package org.zstack.core.cascade;

/**
 */
public class CascadeException extends Exception {
    public CascadeException() {
    }

    public CascadeException(String message) {
        super(message);
    }

    public CascadeException(String message, Throwable cause) {
        super(message, cause);
    }

    public CascadeException(Throwable cause) {
        super(cause);
    }
}
