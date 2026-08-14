package org.zstack.header.vmscheduling;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestRequest(
        path = "/list/vmSchedulingRules/from/conflict/state",
        method = HttpMethod.POST,
        responseClass = APIListVmSchedulingRulesFromExecuteStateReply.class,
        parameterName = "params"
)
public class APIListVmSchedulingRulesFromExecuteStateMsg extends APISyncCallMessage {
    @APIParam
    private List<String> executeStates;

    public List<String> getExecuteStates() {
        return executeStates;
    }

    public void setExecuteStates(List<String> executeStates) {
        this.executeStates = executeStates;
    }

    public static APIListVmSchedulingRulesFromExecuteStateMsg __example__() {
        APIListVmSchedulingRulesFromExecuteStateMsg msg = new APIListVmSchedulingRulesFromExecuteStateMsg();
        msg.setExecuteStates(list("Enabled"));
        return msg;
    }
}
