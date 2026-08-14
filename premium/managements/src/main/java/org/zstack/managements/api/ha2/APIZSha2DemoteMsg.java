package org.zstack.managements.api.ha2;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/management-nodes/zsha2/demote",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIZSha2DemoteEvent.class
)
public class APIZSha2DemoteMsg extends APIMessage {
    public static APIZSha2DemoteMsg __example__() {
        return new APIZSha2DemoteMsg();
    }
}
