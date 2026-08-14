package org.zstack.header.allocator;

public class CloudNoAvailableHostException extends Exception {
    public CloudNoAvailableHostException(String msg) {
        super(msg);
    }

    public CloudNoAvailableHostException(String msg, Throwable t) {
        super(msg, t);
    }
}
