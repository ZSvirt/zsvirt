package org.zstack.scheduler;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.scheduler.CreateSchedulerJobDescMsg;
import org.zstack.header.scheduler.SchedulerJobVO;

import java.util.Map;

/**
 * Created by AlanJager on 2017/6/10.
 */
@RestRequest(
        path = "/scheduler/jobs",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APICreateSchedulerJobEvent.class
)
public class APICreateSchedulerJobMsg extends APICreateMessage implements APIAuditor, CreateSchedulerJobDescMsg {
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam
    private String targetResourceUuid;
    @APIParam(validValues = {"startVm", "stopVm", "rebootVm", "volumeSnapshot", "volumeSnapshotGroup", "volumeBackup",
            "rootVolumeBackup", "vmBackup", "databaseBackup", "localRaidSelfTest", "runAutoScalingGroup",
            "cancelIAM2ProjectLoginExpired", "takeIAM2ProjectLoginExpired"})
    private String type;
    @APIParam(required = false)
    private Map<String, String> parameters;

    public String getTargetResourceUuid() {
        return this.targetResourceUuid;
    }

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

    public void setTargetResourceUuid(String targetResourceUuid) {
        this.targetResourceUuid = targetResourceUuid;
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

    public static APICreateSchedulerJobMsg __example__ () {
        APICreateSchedulerJobMsg msg = new APICreateSchedulerJobMsg();
        msg.setName("job");
        msg.setType("startVm");
        msg.setTargetResourceUuid(uuid());
        msg.setDescription("description");

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateSchedulerJobEvent)rsp).getInventory().getUuid() : "", SchedulerJobVO.class);
    }
}
