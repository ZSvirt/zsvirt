package org.zstack.scheduler;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIBatchRequest;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.scheduler.SchedulerJobGroupVO;
import org.zstack.header.scheduler.SchedulerJobVO;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestRequest(
        path = "/scheduler/jobgroups/{schedulerJobGroupUuid}/job",
        method = HttpMethod.POST,
        responseClass = APIAddSchedulerJobsToSchedulerJobGroupEvent.class,
        parameterName = "params"
)
public class APIAddSchedulerJobsToSchedulerJobGroupMsg extends APIMessage implements SchedulerJobGroupMessage, APIBatchRequest {
    @APIParam(resourceType = SchedulerJobGroupVO.class)
    private String schedulerJobGroupUuid;

    @APIParam(nonempty = true, resourceType = SchedulerJobVO.class)
    private List<String> schedulerJobUuids;

    @APIParam(required = false)
    private Map<String, Integer> priorities;

    public APIAddSchedulerJobsToSchedulerJobGroupMsg() {
    }

    public APIAddSchedulerJobsToSchedulerJobGroupMsg(String jobGroupUuid, List<String> jobUuids) {
        super();
        this.schedulerJobGroupUuid = jobGroupUuid;
        this.schedulerJobUuids = jobUuids;
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

    public Map<String, Integer> getPriorities() {
        return priorities;
    }

    public void setPriorities(Map<String, Integer> priorities) {
        this.priorities = priorities;
    }

    public static APIAddSchedulerJobsToSchedulerJobGroupMsg __example__() {
        APIAddSchedulerJobsToSchedulerJobGroupMsg msg = new APIAddSchedulerJobsToSchedulerJobGroupMsg();
        msg.setSchedulerJobGroupUuid(uuid());
        msg.setSchedulerJobUuids(Collections.singletonList(uuid()));

        return msg;
    }

    @Override
    public Result collectResult(APIMessage message, APIEvent rsp) {
        return new APIBatchRequest.Result(
                ((APIAddSchedulerJobsToSchedulerJobGroupMsg) message).getSchedulerJobUuids().size(),
                ((APIAddSchedulerJobsToSchedulerJobGroupEvent) rsp).getInventories().size()
        );
    }
}
