package org.zstack.header.core.external.service;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/external/services",
        method = HttpMethod.GET,
        responseClass = APIGetExternalServicesReply.class
)
public class APIGetExternalServicesMsg extends APISyncCallMessage {
    public static APIGetExternalServicesMsg __example__() {
        return new APIGetExternalServicesMsg();
    }
}
