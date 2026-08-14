package org.zstack.header.vmscheduling;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestRequest(
        path = "/get/vms/schedulingState/from/SchedulingRule",
        method = HttpMethod.POST,
        responseClass = APIGetVmsSchedulingStateFromSchedulingRuleReply.class,
        parameterName = "params"
)
public class APIGetVmsSchedulingStateFromSchedulingRuleMsg extends APISyncCallMessage {
    @APIParam(resourceType = VmSchedulingRuleVO.class)
    private String ruleUuid;
    @APIParam(resourceType = VmInstanceVO.class)
    private List<String> vmUuids;

    public String getRuleUuid() {
        return ruleUuid;
    }

    public void setRuleUuid(String ruleUuid) {
        this.ruleUuid = ruleUuid;
    }

    public List<String> getVmUuids() {
        return vmUuids;
    }

    public void setVmUuids(List<String> vmUuids) {
        this.vmUuids = vmUuids;
    }

    public static APIGetVmsSchedulingStateFromSchedulingRuleMsg __example__() {
        APIGetVmsSchedulingStateFromSchedulingRuleMsg msg = new APIGetVmsSchedulingStateFromSchedulingRuleMsg();
        msg.setRuleUuid(uuid(VmSchedulingRuleVO.class));
        msg.setVmUuids(list(uuid(VmInstanceVO.class)));
        return msg;
    }
}
