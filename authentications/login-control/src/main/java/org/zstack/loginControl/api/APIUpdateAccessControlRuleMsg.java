package org.zstack.loginControl.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.loginControl.entity.AccessControlRuleVO;

@RestRequest(
        path = "/login-control/access-control/rules/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIUpdateAccessControlRuleEvent.class
)
public class APIUpdateAccessControlRuleMsg extends APIMessage {
    @APIParam(resourceType = AccessControlRuleVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(required = false)
    private String rule;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public static APIUpdateAccessControlRuleMsg __example__() {
        APIUpdateAccessControlRuleMsg msg = new APIUpdateAccessControlRuleMsg();
        msg.setName("rule1");
        msg.setDescription("this is a rule");
        return msg;
    }

}
