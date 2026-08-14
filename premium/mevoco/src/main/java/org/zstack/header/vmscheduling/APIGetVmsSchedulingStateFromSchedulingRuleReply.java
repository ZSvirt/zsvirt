package org.zstack.header.vmscheduling;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import java.util.Map;

@RestResponse(allTo = "ruleMapState")
public class APIGetVmsSchedulingStateFromSchedulingRuleReply extends APIReply {
    private Map<String, VmSchedulingRuleExecuteState> ruleMapState;

    public Map<String, VmSchedulingRuleExecuteState> getRuleMapState() {
        return ruleMapState;
    }

    public void setRuleMapState(Map<String, VmSchedulingRuleExecuteState> ruleMapState) {
        this.ruleMapState = ruleMapState;
    }
}
