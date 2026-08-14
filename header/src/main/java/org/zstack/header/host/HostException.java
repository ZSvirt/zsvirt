package org.zstack.header.host;

public class HostException extends Exception {
    public HostException(String msg, Throwable t) {
        super(msg, t);
    }

    public HostException(String msg) {
        super(msg);
    }

    public HostException(Throwable t) {
        super(t);
    }
}
