package org.zstack.core.gc;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by xing5 on 2017/3/5.
 */
@RestRequest(
        path = "/gc-jobs/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APITriggerGCJobEvent.class
)
public class APITriggerGCJobMsg extends APIMessage implements GarbageCollectorMessage {
    @APIParam(resourceType = GarbageCollectorVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getGCJobUuid() {
        return uuid;
    }
    public static APITriggerGCJobMsg __example__() {
        APITriggerGCJobMsg msg = new APITriggerGCJobMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
