package org.zstack.header.bootstrap;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/mini-clusters/candidate-hosts",
        method = HttpMethod.GET,
        responseClass = APIGetCandidateMiniHostsReply.class
)
public class APIGetCandidateMiniHostsMsg extends APISyncCallMessage {
    @APIParam(required = false)
    private boolean local;

    @APIParam(required = false)
    private boolean configure;

    public boolean isLocal() {
        return local;
    }

    public void setLocal(boolean local) {
        this.local = local;
    }

    public boolean isConfigure() {
        return configure;
    }

    public void setConfigure(boolean configure) {
        this.configure = configure;
    }

    public static APIGetCandidateMiniHostsMsg __example__() {
        APIGetCandidateMiniHostsMsg msg = new APIGetCandidateMiniHostsMsg();
        return msg;
    }
}
