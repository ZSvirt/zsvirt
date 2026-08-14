package org.zstack.scheduler;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.scheduler.SchedulerTriggerVO;

/**
 * Created by AlanJager on 2017/6/8.
 */

@RestRequest(
        path = "/scheduler/triggers/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteSchedulerTriggerEvent.class
)
public class APIDeleteSchedulerTriggerMsg extends APIDeleteMessage {
    @APIParam(resourceType = SchedulerTriggerVO.class, successIfResourceNotExisting = true)
    private String uuid;


    public APIDeleteSchedulerTriggerMsg() {

    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }



    public static APIDeleteSchedulerTriggerMsg __example__() {
        APIDeleteSchedulerTriggerMsg msg = new APIDeleteSchedulerTriggerMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
