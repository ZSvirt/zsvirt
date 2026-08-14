package org.zstack.scheduler;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.scheduler.SchedulerJobVO;
import org.zstack.header.scheduler.SchedulerTriggerVO;

/**
 * Created by AlanJager on 2017/6/8.
 */

@RestRequest(
        path = "/scheduler/jobs/{schedulerJobUuid}/scheduler/triggers/{schedulerTriggerUuid}",
        method = HttpMethod.POST,
        responseClass = APIAddSchedulerJobToSchedulerTriggerEvent.class,
        parameterName = "params"
)
public class APIAddSchedulerJobToSchedulerTriggerMsg extends APIMessage implements SchedulerMessage {
    @APIParam(resourceType = SchedulerJobVO.class)
    private String schedulerJobUuid;

    @APIParam(resourceType = SchedulerTriggerVO.class)
    private String schedulerTriggerUuid;

    @APIParam(required = false)
    private boolean triggerNow;

    public APIAddSchedulerJobToSchedulerTriggerMsg() {
    }

    public APIAddSchedulerJobToSchedulerTriggerMsg(String schedulerJobUuid, String schedulerTriggerUuid) {
        super();
        this.schedulerJobUuid = schedulerJobUuid;
        this.schedulerTriggerUuid = schedulerTriggerUuid;
    }

    public String getSchedulerJobUuid() {
        return schedulerJobUuid;
    }

    public void setSchedulerJobUuid(String schedulerJobUuid) {
        this.schedulerJobUuid = schedulerJobUuid;
    }

    public String getSchedulerTriggerUuid() {
        return schedulerTriggerUuid;
    }

    public void setSchedulerTriggerUuid(String schedulerTriggerUuid) {
        this.schedulerTriggerUuid = schedulerTriggerUuid;
    }

    public static APIAddSchedulerJobToSchedulerTriggerMsg __example__() {
        APIAddSchedulerJobToSchedulerTriggerMsg msg = new APIAddSchedulerJobToSchedulerTriggerMsg();
        msg.setSchedulerJobUuid(uuid());
        msg.setSchedulerTriggerUuid(uuid());

        return msg;
    }

    public boolean isTriggerNow() {
        return triggerNow;
    }

    public void setTriggerNow(boolean triggerNow) {
        this.triggerNow = triggerNow;
    }

    @Override
    public String getSchedulerUuid() {
        return schedulerJobUuid;
    }

    public String getTriggerNowId() {
        return triggerNow ? getId() : null;
    }
}
