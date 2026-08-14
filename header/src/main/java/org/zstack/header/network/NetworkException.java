package org.zstack.header.network;

public class NetworkException extends Exception {
    public NetworkException(String msg, Throwable t) {
        super(msg, t);
    }

    public NetworkException(Throwable t) {
        super(t);
    }

    public NetworkException(String msg) {
        super(msg);
    }
}
