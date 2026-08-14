package org.zstack.zops.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/zops/chrony/servers",
        method = HttpMethod.GET,
        responseClass = APIGetChronyServersReply.class
)
public class APIGetChronyServersMsg extends APISyncCallMessage {
    public static APIGetChronyServersMsg __example__() {
        return new APIGetChronyServersMsg();
    }
}
