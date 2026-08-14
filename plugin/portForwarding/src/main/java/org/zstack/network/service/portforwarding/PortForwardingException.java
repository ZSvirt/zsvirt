package org.zstack.network.service.portforwarding;

public class PortForwardingException extends Exception {
    public PortForwardingException(String msg, Throwable t) {
        super(msg, t);
    }
    
    public PortForwardingException( Throwable t) {
        super(t);
    }
    
    public PortForwardingException(String msg) {
        super(msg);
    }
}
