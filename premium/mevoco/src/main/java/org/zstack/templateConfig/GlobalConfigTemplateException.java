package org.zstack.templateConfig;

public class GlobalConfigTemplateException extends RuntimeException {
    public GlobalConfigTemplateException(String msg, Throwable t) {
        super(msg, t);
    }

    public GlobalConfigTemplateException(String msg) {
        super(msg);
    }

    public GlobalConfigTemplateException(Throwable t) {
        super(t);
    }
}