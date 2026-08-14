package org.zstack.header.network.l2;

/**
 */
public class L2NetworkException extends Exception {
    public L2NetworkException() {
    }

    public L2NetworkException(String message) {
        super(message);
    }

    public L2NetworkException(String message, Throwable cause) {
        super(message, cause);
    }

    public L2NetworkException(Throwable cause) {
        super(cause);
    }
}
