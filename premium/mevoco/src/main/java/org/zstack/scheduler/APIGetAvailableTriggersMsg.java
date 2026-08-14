package org.zstack.scheduler;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by kayo on 2017/9/8.
 */
@RestRequest(
        path = "/scheduler/triggers/available",
        method = HttpMethod.GET,
        responseClass = APIGetAvailableTriggersReply.class
)
public class APIGetAvailableTriggersMsg extends APISyncCallMessage {
    public static APIGetAvailableTriggersMsg __example__() {
        APIGetAvailableTriggersMsg msg = new APIGetAvailableTriggersMsg();
        return msg;
    }
}
