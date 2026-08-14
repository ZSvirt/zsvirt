package org.zstack.expon.sdk;

public class ExponApiException extends RuntimeException {
    public ExponApiException(String format) {
        super(format);
    }

    public ExponApiException(Exception e) {
        super(e);
    }
}
