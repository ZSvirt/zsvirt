package org.zstack.core.singleflight;

import org.zstack.header.message.NeedReplyMessage;

public class ExternalSingleFlightMsg extends NeedReplyMessage {
    private String resourceUuid;
    private String method;

    // do not contains ReturnValueCompletion
    private Object[] args;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }
}
