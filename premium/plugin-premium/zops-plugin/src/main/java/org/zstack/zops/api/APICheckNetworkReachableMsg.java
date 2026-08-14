package org.zstack.zops.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@RestRequest(
        path = "/zops/check/network",
        responseClass = APICheckNetworkReachableReply.class,
        method = HttpMethod.GET
)
public class APICheckNetworkReachableMsg extends APISyncCallMessage {
    @APIParam(required = false, nonempty = true)
    private List<String> sourceHostnames;

    @APIParam(nonempty = true)
    private List<String> targetHostnames;

    public List<String> getSourceHostnames() {
        return sourceHostnames;
    }

    public void setSourceHostnames(List<String> sourceHostnames) {
        this.sourceHostnames = sourceHostnames;
    }

    public List<String> getTargetHostnames() {
        return targetHostnames;
    }

    public void setTargetHostnames(List<String> targetHostnames) {
        this.targetHostnames = targetHostnames;
    }

    public static APICheckNetworkReachableMsg __example__() {
        APICheckNetworkReachableMsg msg = new APICheckNetworkReachableMsg();

        msg.setSourceHostnames(asList("172.0.0.1"));
        msg.setTargetHostnames(asList("test1.com", "test2.com"));
        return msg;
    }
}
