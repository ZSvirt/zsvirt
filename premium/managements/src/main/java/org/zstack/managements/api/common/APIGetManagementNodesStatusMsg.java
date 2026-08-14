package org.zstack.managements.api.common;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/management-nodes/status",
        method = HttpMethod.GET,
        responseClass = APIGetManagementNodesStatusReply.class
)
public class APIGetManagementNodesStatusMsg extends APISyncCallMessage {
    public static APIGetManagementNodesStatusMsg __example__() {
        return new APIGetManagementNodesStatusMsg();
    }
}
