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
        method = HttpMethod.DELETE,
        responseClass = APIRemoveSchedulerJobFromSchedulerTriggerEvent.class
)
public class APIRemoveSchedulerJobFromSchedulerTriggerMsg extends APIMessage implements SchedulerMessage {
    @APIParam(resourceType = SchedulerJobVO.class)
    private String schedulerJobUuid;

    @APIParam(resourceType = SchedulerTriggerVO.class)
    private String schedulerTriggerUuid;

    public APIRemoveSchedulerJobFromSchedulerTriggerMsg() {
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

    public APIRemoveSchedulerJobFromSchedulerTriggerMsg(String schedulerJobUuid, String schedulerTriggerUuid) {
        super();
        this.schedulerJobUuid = schedulerJobUuid;
        this.schedulerTriggerUuid = schedulerTriggerUuid;
    }

    public static APIRemoveSchedulerJobFromSchedulerTriggerMsg __example__() {
        APIRemoveSchedulerJobFromSchedulerTriggerMsg msg = new APIRemoveSchedulerJobFromSchedulerTriggerMsg();
        msg.setSchedulerJobUuid(uuid());
        msg.setSchedulerTriggerUuid(uuid());
        return msg;
    }

    @Override
    public String getSchedulerUuid() {
        return schedulerJobUuid;
    }
}
