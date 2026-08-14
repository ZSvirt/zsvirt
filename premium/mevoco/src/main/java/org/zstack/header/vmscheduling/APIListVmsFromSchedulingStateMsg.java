package org.zstack.header.vmscheduling;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestRequest(
        path = "/list/vms/from/executeState",
        method = HttpMethod.POST,
        responseClass = APIListVmsFromSchedulingStateReply.class,
        parameterName = "params"
)
public class APIListVmsFromSchedulingStateMsg extends APISyncCallMessage {
    @APIParam(resourceType = VmSchedulingRuleVO.class)
    private String ruleUuid;
    @APIParam
    private List<String> executeStates;

    public String getRuleUuid() {
        return ruleUuid;
    }

    public void setRuleUuid(String ruleUuid) {
        this.ruleUuid = ruleUuid;
    }

    public List<String> getExecuteStates() {
        return executeStates;
    }

    public void setExecuteStates(List<String> executeStates) {
        this.executeStates = executeStates;
    }

    public static APIListVmsFromSchedulingStateMsg __example__() {
        APIListVmsFromSchedulingStateMsg msg = new APIListVmsFromSchedulingStateMsg();
        msg.setRuleUuid(uuid(VmSchedulingRuleVO.class));
        msg.setExecuteStates(list("SCHEDULED", "EXECUTING"));
        return msg;
    }
}
