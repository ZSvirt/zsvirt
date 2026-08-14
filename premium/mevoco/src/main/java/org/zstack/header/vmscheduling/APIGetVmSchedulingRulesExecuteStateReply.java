package org.zstack.header.vmscheduling;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestResponse(allTo = "ruleMapState")
public class APIGetVmSchedulingRulesExecuteStateReply extends APIReply {
    private Map<String, VmSchedulingRuleExecuteState> ruleMapState;

    public Map<String, VmSchedulingRuleExecuteState> getRuleMapState() {
        return ruleMapState;
    }

    public void setRuleMapState(Map<String, VmSchedulingRuleExecuteState> ruleMapState) {
        this.ruleMapState = ruleMapState;
    }

    public static APIGetVmSchedulingRulesExecuteStateReply __example__() {
        APIGetVmSchedulingRulesExecuteStateReply reply = new APIGetVmSchedulingRulesExecuteStateReply();
        Map<String, VmSchedulingRuleExecuteState> ruleMapState = new HashMap<>();
        ruleMapState.put(uuid(), VmSchedulingRuleExecuteState.Normal);
        reply.setRuleMapState(ruleMapState);
        return reply;
    }
}
