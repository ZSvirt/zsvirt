package org.zstack.core.config;

public class GlobalConfigException extends RuntimeException {
    public GlobalConfigException(String msg, Throwable t) {
        super(msg, t);
    }
    
    public GlobalConfigException(String msg) {
        super(msg);
    }
    
    public GlobalConfigException(Throwable t) {
        super(t);
    }
}
