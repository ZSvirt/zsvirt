package org.zstack.templateConfig;

public class TemplateConfigException extends RuntimeException {
    public TemplateConfigException(String msg, Throwable t) {
        super(msg, t);
    }

    public TemplateConfigException(String msg) {
        super(msg);
    }

    public TemplateConfigException(Throwable t) {
        super(t);
    }
}
