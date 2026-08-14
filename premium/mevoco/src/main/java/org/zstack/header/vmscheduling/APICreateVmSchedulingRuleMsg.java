package org.zstack.header.vmscheduling;

import org.springframework.http.HttpMethod;
import org.zstack.header.affinitygroup.APICreateAffinityGroupEvent;
import org.zstack.header.affinitygroup.APICreateAffinityGroupMsg;
import org.zstack.header.configuration.APICreateInstanceOfferingEvent;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/vmsSchedulingRule",
        method = HttpMethod.POST,
        responseClass = APICreateAffinityGroupEvent.class,
        parameterName = "params"
)
public class APICreateVmSchedulingRuleMsg extends APICreateAffinityGroupMsg implements APIAuditor {
    @APIParam(validValues = {"AFFINITY", "ANTIAFFINITY"})
    private String rule;
    @APIParam(validValues = {"SOFT", "HARD"})
    private String mode;
    @APIParam
    private String vmGroupUuid;
    @APIParam(required = false)
    private String hostGroupUuid;

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getVmGroupUuid() {
        return vmGroupUuid;
    }

    public void setVmGroupUuid(String vmGroupUuid) {
        this.vmGroupUuid = vmGroupUuid;
    }

    public String getHostGroupUuid() {
        return hostGroupUuid;
    }

    public void setHostGroupUuid(String hostGroupUuid) {
        this.hostGroupUuid = hostGroupUuid;
    }

    public static APICreateVmSchedulingRuleMsg __example__() {
        APICreateVmSchedulingRuleMsg msg = new APICreateVmSchedulingRuleMsg();
        msg.setName("vm-scheduling-rule");
        msg.setDescription("desc");
        msg.setMode("SOFT");
        msg.setRule("AFFINITY");
        msg.setHostGroupUuid(uuid());
        msg.setVmGroupUuid(uuid());
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        String uuid = "";
        if (rsp.isSuccess()) {
            APICreateAffinityGroupEvent evt = (APICreateAffinityGroupEvent) rsp;
            uuid = evt.getInventory().getUuid();
        }
        return new Result(uuid, VmSchedulingRuleVO.class);
    }
}
