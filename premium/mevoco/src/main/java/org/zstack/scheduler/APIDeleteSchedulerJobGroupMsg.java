package org.zstack.scheduler;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.scheduler.SchedulerJobGroupVO;

@RestRequest(
        path = "/scheduler/jobgroups/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteSchedulerJobGroupEvent.class
)
public class APIDeleteSchedulerJobGroupMsg extends APIDeleteMessage implements SchedulerJobGroupMessage {

    @APIParam(resourceType = SchedulerJobGroupVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public APIDeleteSchedulerJobGroupMsg() {
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDeleteSchedulerJobGroupMsg __example__() {
        APIDeleteSchedulerJobGroupMsg msg = new APIDeleteSchedulerJobGroupMsg();
        msg.setUuid(uuid());
        return msg;
    }

    @Override
    public String getSchedulerJobGroupUuid() {
        return uuid;
    }
}
