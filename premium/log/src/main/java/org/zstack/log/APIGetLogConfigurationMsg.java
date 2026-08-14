package org.zstack.log;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/log/configurations",
        method = HttpMethod.GET,
        responseClass = APIGetLogConfigurationReply.class
)
public class APIGetLogConfigurationMsg extends APISyncCallMessage {
    public static APIGetLogConfigurationMsg __example__() {
        APIGetLogConfigurationMsg msg = new APIGetLogConfigurationMsg();

        return msg;
    }
}
