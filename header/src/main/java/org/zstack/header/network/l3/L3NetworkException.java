package org.zstack.header.network.l3;

/**
 */
public class L3NetworkException extends Exception {
    public L3NetworkException() {
    }

    public L3NetworkException(String message) {
        super(message);
    }

    public L3NetworkException(String message, Throwable cause) {
        super(message, cause);
    }

    public L3NetworkException(Throwable cause) {
        super(cause);
    }
}
