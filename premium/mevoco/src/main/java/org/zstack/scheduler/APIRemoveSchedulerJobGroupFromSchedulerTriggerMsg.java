package org.zstack.scheduler;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.scheduler.SchedulerJobGroupVO;
import org.zstack.header.scheduler.SchedulerTriggerVO;

@RestRequest(
        path = "/scheduler/jobgroups/{schedulerJobGroupUuid}/scheduler/triggers/{schedulerTriggerUuid}",
        method = HttpMethod.DELETE,
        responseClass = APIRemoveSchedulerJobGroupFromSchedulerTriggerEvent.class
)
public class APIRemoveSchedulerJobGroupFromSchedulerTriggerMsg extends APIMessage implements SchedulerJobGroupMessage {
    @APIParam(resourceType = SchedulerJobGroupVO.class)
    private String schedulerJobGroupUuid;

    @APIParam(resourceType = SchedulerTriggerVO.class)
    private String schedulerTriggerUuid;

    public APIRemoveSchedulerJobGroupFromSchedulerTriggerMsg() {
    }

    @Override
    public String getSchedulerJobGroupUuid() {
        return schedulerJobGroupUuid;
    }

    public void setSchedulerJobGroupUuid(String schedulerJobGroupUuid) {
        this.schedulerJobGroupUuid = schedulerJobGroupUuid;
    }

    public String getSchedulerTriggerUuid() {
        return schedulerTriggerUuid;
    }

    public void setSchedulerTriggerUuid(String schedulerTriggerUuid) {
        this.schedulerTriggerUuid = schedulerTriggerUuid;
    }

    public APIRemoveSchedulerJobGroupFromSchedulerTriggerMsg(String schedulerJobGroupUuid, String schedulerTriggerUuid) {
        super();
        this.schedulerJobGroupUuid = schedulerJobGroupUuid;
        this.schedulerTriggerUuid = schedulerTriggerUuid;
    }

    public static APIRemoveSchedulerJobGroupFromSchedulerTriggerMsg __example__() {
        APIRemoveSchedulerJobGroupFromSchedulerTriggerMsg msg = new APIRemoveSchedulerJobGroupFromSchedulerTriggerMsg();
        msg.setSchedulerJobGroupUuid(uuid());
        msg.setSchedulerTriggerUuid(uuid());
        return msg;
    }
}
