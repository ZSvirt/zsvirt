package org.zstack.header.managementnode;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by Qi Le on 2021/3/15
 */
@RestRequest(
        path = "/management-nodes/platform-timezone",
        method = HttpMethod.GET,
        responseClass = APIGetPlatformTimeZoneReply.class
)
public class APIGetPlatformTimeZoneMsg extends APISyncCallMessage implements APIManagementNodeMessage {
    public static APIGetPlatformTimeZoneMsg __example__() {
        APIGetPlatformTimeZoneMsg msg = new APIGetPlatformTimeZoneMsg();
        return msg;
    }
}
