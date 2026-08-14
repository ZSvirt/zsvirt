package org.zstack.header.exception;

public class CloudFactoryFailureException extends CloudRuntimeException {
    private String details;

    public CloudFactoryFailureException(String message, String details) {
        super(message);
        this.details = details;
    }

    public CloudFactoryFailureException(String message, String details, Throwable t) {
        super(message, t);
        this.details = details;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

}
