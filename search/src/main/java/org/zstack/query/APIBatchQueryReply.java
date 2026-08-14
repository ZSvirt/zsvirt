package org.zstack.query;

import org.zstack.header.message.APIReply;
import org.zstack.header.message.NoJsonSchema;
import org.zstack.header.rest.RestResponse;

import java.util.Map;

@RestResponse(allTo = "result")
public class APIBatchQueryReply extends APIReply {
    @NoJsonSchema
    private Map<String, Object> result;

    public Map<String, Object> getResult() {
        return result;
    }

    public void setResult(Map<String, Object> result) {
        this.result = result;
    }

    public static APIBatchQueryReply __example__() {
        APIBatchQueryReply ret = new APIBatchQueryReply();
        return ret;
    }
}
