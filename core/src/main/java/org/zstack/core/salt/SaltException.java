package org.zstack.core.salt;

/**
 */
public class SaltException extends RuntimeException {
    public SaltException(String message) {
        super(message);
    }

    public SaltException(String message, Throwable cause) {
        super(message, cause);
    }

    public SaltException(Throwable cause) {
        super(cause);
    }
}
