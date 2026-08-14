package org.zstack.scheduler;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.scheduler.SchedulerJobGroupVO;
import org.zstack.header.scheduler.SchedulerJobVO;

import java.util.Collections;
import java.util.List;

@RestRequest(
        path = "/scheduler/jobgroups/{schedulerJobGroupUuid}/job",
        method = HttpMethod.DELETE,
        responseClass = APIRemoveSchedulerJobsFromSchedulerJobGroupEvent.class
)
public class APIRemoveSchedulerJobsFromSchedulerJobGroupMsg extends APIMessage implements SchedulerJobGroupMessage {
    @APIParam(resourceType = SchedulerJobGroupVO.class)
    private String schedulerJobGroupUuid;

    @APIParam(resourceType = SchedulerJobVO.class)
    private List<String> schedulerJobUuids;

    public APIRemoveSchedulerJobsFromSchedulerJobGroupMsg() {
    }

    public APIRemoveSchedulerJobsFromSchedulerJobGroupMsg(String schedulerJobGroupUuid, List<String> schedulerJobUuids) {
        super();
        this.schedulerJobGroupUuid = schedulerJobGroupUuid;
        this.schedulerJobUuids = schedulerJobUuids;
    }

    @Override
    public String getSchedulerJobGroupUuid() {
        return schedulerJobGroupUuid;
    }

    public void setSchedulerJobGroupUuid(String schedulerJobGroupUuid) {
        this.schedulerJobGroupUuid = schedulerJobGroupUuid;
    }

    public List<String> getSchedulerJobUuids() {
        return schedulerJobUuids;
    }

    public void setSchedulerJobUuids(List<String> schedulerJobUuids) {
        this.schedulerJobUuids = schedulerJobUuids;
    }

    public static APIRemoveSchedulerJobsFromSchedulerJobGroupMsg __example__() {
        APIRemoveSchedulerJobsFromSchedulerJobGroupMsg msg = new APIRemoveSchedulerJobsFromSchedulerJobGroupMsg();
        msg.setSchedulerJobGroupUuid(uuid());
        msg.setSchedulerJobUuids(Collections.singletonList(uuid()));

        return msg;
    }
}
