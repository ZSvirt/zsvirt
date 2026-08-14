package org.zstack.header.exception;

public class CloudConfigureFailException extends CloudRuntimeException {
    private static final long serialVersionUID = SerialVersionUID.CloudConfigureFailException;

    public CloudConfigureFailException(Class<?> clazz, String msg) {
        super(clazz.getName() + " failed to configure because " + msg);
    }

    public CloudConfigureFailException(Class<?> clazz, String msg, Exception cause) {
        super(clazz.getName() + " failed to configure because " + msg, cause);
    }
}
