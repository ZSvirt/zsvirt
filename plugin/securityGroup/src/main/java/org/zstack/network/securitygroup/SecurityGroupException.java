package org.zstack.network.securitygroup;

public class SecurityGroupException extends Exception {
    public SecurityGroupException(String err, Throwable t) {
        super(err, t);
    }
    
    public SecurityGroupException(Throwable t) {
        super(t);
    }
    
    public SecurityGroupException(String err) {
        super(err);
    }
}
