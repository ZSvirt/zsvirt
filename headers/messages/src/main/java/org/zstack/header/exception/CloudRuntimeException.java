package org.zstack.header.exception;

public class CloudRuntimeException extends RuntimeException implements CloudException {
    private static final long serialVersionUID = SerialVersionUID.CloudRuntimeException;

    public CloudRuntimeException(Throwable th) {
        super(th);
    }

    public CloudRuntimeException(String message) {
        super(message);
    }

    public CloudRuntimeException(String message, Throwable th) {
        super(message, th);
    }

    protected CloudRuntimeException() {
        super();
    }
}
