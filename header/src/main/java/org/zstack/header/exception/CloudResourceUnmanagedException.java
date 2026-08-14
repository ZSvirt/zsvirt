package org.zstack.header.exception;

public class CloudResourceUnmanagedException extends CloudRuntimeException {
    public CloudResourceUnmanagedException(String msg) {
        super(msg);
    }

    public CloudResourceUnmanagedException(String msg, Throwable t) {
        super(msg, t);
    }
}
