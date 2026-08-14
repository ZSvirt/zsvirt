package org.zstack.header.core.external.service;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/external/services",
        isAction = true,
        responseClass = APIReloadExternalServiceEvent.class,
        method = HttpMethod.PUT
)
public class APIReloadExternalServiceMsg extends APIMessage {
    @APIParam
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static APIReloadExternalServiceMsg __example__() {
        APIReloadExternalServiceMsg msg = new APIReloadExternalServiceMsg();
        msg.setName("prometheus");
        return msg;
    }
}
