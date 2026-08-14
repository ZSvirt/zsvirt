package org.zstack.managements.api.ha2;


import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/management-nodes/zsha2/status",
        method = HttpMethod.GET,
        responseClass = APIGetZSha2StatusReply.class
)
public class APIGetZSha2StatusMsg extends APISyncCallMessage {
    public static APIGetZSha2StatusMsg __example__() {
        return new APIGetZSha2StatusMsg();
    }
}
