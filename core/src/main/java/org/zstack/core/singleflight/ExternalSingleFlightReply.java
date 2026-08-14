package org.zstack.core.singleflight;


import org.zstack.header.message.MessageReply;

public class ExternalSingleFlightReply extends MessageReply {
    private Object result;

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
