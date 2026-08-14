package org.zstack.scheduler;

import org.springframework.http.HttpMethod;
import org.zstack.core.Platform;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.other.APIMultiAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.scheduler.SchedulerJobGroupVO;
import org.zstack.header.scheduler.SchedulerJobVO;
import org.zstack.header.scheduler.SchedulerTriggerVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by MaJin on 2019/4/17.
 */
@RestRequest(
        path = "/scheduler/triggers/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIRunSchedulerTriggerEvent.class
)
public class APIRunSchedulerTriggerMsg extends APIMessage implements APIMultiAuditor {
    @APIParam(resourceType = SchedulerTriggerVO.class)
    private String uuid;

    @APIParam(resourceType = SchedulerJobVO.class, required = false)
    private List<String> jobUuids;

    @APINoSee
    private Set<String> jobGroupUuids;

    @APINoSee
    private Set<String> jobUuidsInGroup;

    @APINoSee
    private Set<String> jobUuidsNotInGroup;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public List<String> getJobUuids() {
        return jobUuids;
    }

    public void setJobUuids(List<String> jobUuids) {
        this.jobUuids = jobUuids;
    }

    public void setJobGroupUuids(Set<String> jobGroupUuids) {
        this.jobGroupUuids = jobGroupUuids;
    }

    public Set<String> getJobGroupUuids() {
        return jobGroupUuids;
    }

    public Set<String> getJobUuidsInGroup() {
        return jobUuidsInGroup;
    }

    public void setJobUuidsInGroup(Set<String> jobUuidsInGroup) {
        this.jobUuidsInGroup = jobUuidsInGroup;
    }

    public Set<String> getJobUuidsNotInGroup() {
        return jobUuidsNotInGroup;
    }

    public void setJobUuidsNotInGroup(Set<String> jobUuidsNotInGroup) {
        this.jobUuidsNotInGroup = jobUuidsNotInGroup;
    }

    public static APIRunSchedulerTriggerMsg __example__() {
        APIRunSchedulerTriggerMsg result = new APIRunSchedulerTriggerMsg();
        result.uuid = uuid(SchedulerTriggerVO.class);
        return result;
    }

    @Override
    public List<APIAuditor.Result> multiAudit(APIMessage msg, APIEvent rsp) {
        List<APIAuditor.Result> results = new ArrayList<>();
        APIRunSchedulerTriggerMsg rmsg = (APIRunSchedulerTriggerMsg) msg;
        if (rmsg.jobUuids != null) {
            for (String jobUuid : rmsg.jobUuids) {
                results.add(new APIAuditor.Result(jobUuid, SchedulerJobVO.class));
            }
        }

        if (rmsg.jobGroupUuids != null) {
            for (String groupUuid : rmsg.jobGroupUuids) {
                results.add(new APIAuditor.Result(groupUuid, SchedulerJobGroupVO.class));
            }
        }

        return results;
    }
}