package org.zstack.zsv.core.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/zsv/nodes/roles",
        method = HttpMethod.GET,
        responseClass = APIGetNodeRolesReply.class
)
public class APIGetNodeRolesMsg extends APISyncCallMessage {
    public static APIGetNodeRolesMsg __example__() {
        return new APIGetNodeRolesMsg();
    }
}
