package org.zstack.network.securitygroup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.List;
import static java.util.Arrays.asList;

@RestRequest(
        path = "/security-groups/{securityGroupUuid}/rules/priority/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateSecurityGroupRulePriorityEvent.class,
        isAction = true
)
 
public class APIUpdateSecurityGroupRulePriorityMsg extends APIMessage implements SecurityGroupMessage {
    public static class SecurityGroupRulePriorityAO {
        @APIParam(resourceType = SecurityGroupRuleVO.class, nonempty = true, required = true)
        private String ruleUuid;
        @APIParam(required = true, nonempty = true)
        private Integer priority;

        public String getRuleUuid() {
            return ruleUuid;
        }

        public void setRuleUuid(String ruleUuid) {
            this.ruleUuid = ruleUuid;
        }

        public Integer getPriority() {
            return priority;
        }

        public void setPriority(Integer priority) {
            this.priority = priority;
        }
    }

    @APIParam(resourceType = SecurityGroupVO.class, nonempty = true, required = true)
    private String securityGroupUuid;

    @APIParam(required = true, validValues = {"Ingress", "Egress"}, nonempty = true)
    private String type;

    @APIParam(required = true, nonempty = true)
    private List<SecurityGroupRulePriorityAO> rules;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<SecurityGroupRulePriorityAO> getRules() {
        return rules;
    }

    public void setRules(List<SecurityGroupRulePriorityAO> rules) {
        this.rules = rules;
    }

    @Override
    public String getSecurityGroupUuid() {
        return securityGroupUuid;
    }

    public void setSecurityGroupUuid(String securityGroupUuid) {
        this.securityGroupUuid = securityGroupUuid;
    }

    public static APIUpdateSecurityGroupRulePriorityMsg __example__() {
        APIUpdateSecurityGroupRulePriorityMsg msg = new APIUpdateSecurityGroupRulePriorityMsg();
        msg.setSecurityGroupUuid(uuid());
        msg.setType("Ingress");
        SecurityGroupRulePriorityAO ao = new SecurityGroupRulePriorityAO();
        ao.setRuleUuid(uuid());
        ao.setPriority(1);
        msg.setRules(asList(ao));
        return msg;
    }
}
