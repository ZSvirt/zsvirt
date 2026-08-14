package org.zstack.core.config;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/global-configurations/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIResetGlobalConfigEvent.class
)
public class APIResetGlobalConfigMsg extends APIMessage {
    public static APIResetGlobalConfigMsg __example__() {
        APIResetGlobalConfigMsg msg = new APIResetGlobalConfigMsg();
        return msg;
    }
}
