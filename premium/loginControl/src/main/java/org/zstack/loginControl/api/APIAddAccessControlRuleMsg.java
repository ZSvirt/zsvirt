package org.zstack.loginControl.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.loginControl.entity.AccessControlRuleVO;

@RestRequest(
        path = "/login-control/access-control/rules",
        method = HttpMethod.POST,
        responseClass = APIAddAccessControlRuleEvent.class,
        parameterName = "params"
)
public class APIAddAccessControlRuleMsg extends APICreateMessage implements APIAuditor {
    @APIParam
    private String name;

    @APIParam(required = false)
    private String description;

    @APIParam
    private String rule;

    @APIParam
    private String controlStrategy;

    public String getControlStrategy() {
        return controlStrategy;
    }

    public void setControlStrategy(String controlStrategy) {
        this.controlStrategy = controlStrategy;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        String resUuid = "";
        if (rsp.isSuccess()) {
            APIAddAccessControlRuleEvent evt = (APIAddAccessControlRuleEvent) rsp;
            resUuid = evt.getInventory().getUuid();
        }

        return new Result(resUuid, AccessControlRuleVO.class);
    }

    public static APIAddAccessControlRuleMsg __example__() {
        APIAddAccessControlRuleMsg msg = new APIAddAccessControlRuleMsg();
        msg.setName("rule1");
        msg.setRule("192.168.1.1");
        msg.setControlStrategy("REJECT");
        return msg;
    }
}
