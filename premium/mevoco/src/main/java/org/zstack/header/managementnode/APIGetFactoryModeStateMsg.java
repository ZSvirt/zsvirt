package org.zstack.header.managementnode;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/management-nodes/factory-mode-state",
        method = HttpMethod.GET,
        responseClass = APIGetFactoryModeStateReply.class
)
public class APIGetFactoryModeStateMsg extends APISyncCallMessage {
    public static APIGetFactoryModeStateMsg __example__() {
        APIGetFactoryModeStateMsg msg = new APIGetFactoryModeStateMsg();
        return msg;
    }
}
