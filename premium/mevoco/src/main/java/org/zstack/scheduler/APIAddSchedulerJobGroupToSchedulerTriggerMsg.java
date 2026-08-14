package org.zstack.scheduler;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.scheduler.SchedulerJobGroupVO;
import org.zstack.header.scheduler.SchedulerTriggerVO;

@RestRequest(
        path = "/scheduler/jobgroups/{schedulerJobGroupUuid}/scheduler/triggers/{schedulerTriggerUuid}",
        method = HttpMethod.POST,
        responseClass = APIAddSchedulerJobGroupToSchedulerTriggerEvent.class,
        parameterName = "params"
)
public class APIAddSchedulerJobGroupToSchedulerTriggerMsg extends APIMessage implements SchedulerJobGroupMessage {
    @APIParam(resourceType = SchedulerJobGroupVO.class)
    private String schedulerJobGroupUuid;

    @APIParam(resourceType = SchedulerTriggerVO.class)
    private String schedulerTriggerUuid;

    @APIParam(required = false)
    private boolean triggerNow;

    public APIAddSchedulerJobGroupToSchedulerTriggerMsg() {
    }

    public APIAddSchedulerJobGroupToSchedulerTriggerMsg(String schedulerJobGroupUuid, String schedulerTriggerUuid) {
        super();
        this.schedulerJobGroupUuid = schedulerJobGroupUuid;
        this.schedulerTriggerUuid = schedulerTriggerUuid;
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

    public static APIAddSchedulerJobGroupToSchedulerTriggerMsg __example__() {
        APIAddSchedulerJobGroupToSchedulerTriggerMsg msg = new APIAddSchedulerJobGroupToSchedulerTriggerMsg();
        msg.setSchedulerJobGroupUuid(uuid());
        msg.setSchedulerTriggerUuid(uuid());

        return msg;
    }

    public boolean isTriggerNow() {
        return triggerNow;
    }

    public void setTriggerNow(boolean triggerNow) {
        this.triggerNow = triggerNow;
    }

    public String getTriggerNowId() {
        return triggerNow ? getId() : null;
    }
}
