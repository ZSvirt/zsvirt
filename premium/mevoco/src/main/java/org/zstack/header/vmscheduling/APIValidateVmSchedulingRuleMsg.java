package org.zstack.header.vmscheduling;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.zone.ZoneVO;

@RestRequest(
        path = "/validate/vmSchedulingRule",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIValidateVmSchedulingRuleReply.class
)
public class APIValidateVmSchedulingRuleMsg extends APISyncCallMessage {
    @APIParam(resourceType = VmSchedulingRuleGroupVO.class)
    private String vmGroupUuid;
    @APIParam(resourceType = HostSchedulingRuleGroupVO.class, required = false)
    private String hostGroupUuid;
    @APIParam(validValues = {"AFFINITY", "ANTIAFFINITY"})
    private String rule;
    @APIParam(validValues = {"SOFT", "HARD"})
    private String mode;
    @APIParam(resourceType = ZoneVO.class, required = false)
    private String zoneUuid;

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

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public static APIValidateVmSchedulingRuleMsg __example__() {
        APIValidateVmSchedulingRuleMsg msg = new APIValidateVmSchedulingRuleMsg();
        msg.setRule("AFFINITY");
        msg.setMode("HARD");
        msg.setHostGroupUuid(uuid());
        msg.setVmGroupUuid(uuid());
        msg.setZoneUuid(uuid());
        return msg;
    }
}
