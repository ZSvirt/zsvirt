package org.zstack.zql.ast.visitors;

public class ZQLError extends RuntimeException {
    public ZQLError() {
    }

    public ZQLError(String message) {
        super(message);
    }

    public ZQLError(String message, Throwable cause) {
        super(message, cause);
    }

    public ZQLError(Throwable cause) {
        super(cause);
    }

    public ZQLError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
