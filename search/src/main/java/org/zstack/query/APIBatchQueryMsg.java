package org.zstack.query;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(path = "/batch-queries", method = HttpMethod.GET, responseClass = APIBatchQueryReply.class)
@Deprecated
public class APIBatchQueryMsg extends APISyncCallMessage {
    private String script;

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public static APIBatchQueryMsg __example__() {
        APIBatchQueryMsg ret = new APIBatchQueryMsg();
        return ret;
    }
}
