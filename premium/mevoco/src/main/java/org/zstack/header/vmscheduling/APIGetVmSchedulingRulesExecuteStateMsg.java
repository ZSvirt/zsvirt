package org.zstack.header.vmscheduling;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

import java.util.Arrays;
import java.util.List;

@RestRequest(
        path = "/get/vmSchedulingRules/conflict/state",
        method = HttpMethod.POST,
        responseClass = APIGetVmSchedulingRulesExecuteStateReply.class,
        parameterName = "params"
)
public class APIGetVmSchedulingRulesExecuteStateMsg extends APISyncCallMessage {
    @APIParam(resourceType = VmSchedulingRuleVO.class)
    private List<String> uuids;

    public List<String> getUuids() {
        return uuids;
    }

    public void setUuids(List<String> uuids) {
        this.uuids = uuids;
    }

    public static APIGetVmSchedulingRulesExecuteStateMsg __example__() {
        APIGetVmSchedulingRulesExecuteStateMsg msg = new APIGetVmSchedulingRulesExecuteStateMsg();
        msg.setUuids(Arrays.asList(uuid()));
        return msg;
    }
}
