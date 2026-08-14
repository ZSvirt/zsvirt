package org.zstack.kvm;

import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NoJsonSchema;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.LinkedHashMap;

/**
 */
public class KVMHostAsyncHttpCallReply extends MessageReply {
    @NoJsonSchema
    private LinkedHashMap response;

    public LinkedHashMap getResponse() {
        return response;
    }

    public void setResponse(LinkedHashMap response) {
        this.response = response;
    }

    public <T> T toResponse(Class<T> clz) {
        return JSONObjectUtil.rehashObject(response, clz);
    }
}
