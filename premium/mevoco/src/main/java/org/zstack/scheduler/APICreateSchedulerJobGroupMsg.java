package org.zstack.scheduler;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.scheduler.CreateSchedulerJobDescMsg;
import org.zstack.header.scheduler.SchedulerJobGroupVO;

import java.util.Map;

@RestRequest(
        path = "/scheduler/jobgroups",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APICreateSchedulerJobGroupEvent.class
)
public class APICreateSchedulerJobGroupMsg extends APICreateMessage implements APIAuditor, CreateSchedulerJobDescMsg {
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(validValues = {"startVm", "stopVm", "rebootVm", "volumeSnapshot", "volumeSnapshotGroup", "volumeBackup",
            "rootVolumeBackup", "vmBackup", "databaseBackup", "runAutoScalingGroup", "cancelIAM2ProjectLoginExpired",
            "takeIAM2ProjectLoginExpired"})
    private String type;
    @APIParam(required = false)
    private Map<String, String> parameters;

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public Map<String, String> getParameters() {
        return parameters;
    }

    @Override
    public String getAccountUuid() {
        return getSession().getAccountUuid();
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public static APICreateSchedulerJobGroupMsg __example__ () {
        APICreateSchedulerJobGroupMsg msg = new APICreateSchedulerJobGroupMsg();
        msg.setName("job");
        msg.setDescription("description");
        msg.setType("startVm");

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateSchedulerJobGroupEvent)rsp).getInventory().getUuid() : "", SchedulerJobGroupVO.class);
    }
}
