package org.zstack.scheduler;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.scheduler.SchedulerJobVO;

/**
 * Created by Mei Lei<meilei007@gmail.com> on 7/15/16.
 */
@RestRequest(
        path = "/scheduler/jobs/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteSchedulerJobEvent.class
)
public class APIDeleteSchedulerJobMsg extends APIDeleteMessage implements SchedulerMessage {

    @APIParam(resourceType = SchedulerJobVO.class, successIfResourceNotExisting = true)
    private String uuid;


    public APIDeleteSchedulerJobMsg() {

    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }


 
    public static APIDeleteSchedulerJobMsg __example__() {
        APIDeleteSchedulerJobMsg msg = new APIDeleteSchedulerJobMsg();
        msg.setUuid(uuid());
        return msg;
    }

    @Override
    public String getSchedulerUuid() {
        return uuid;
    }
}
