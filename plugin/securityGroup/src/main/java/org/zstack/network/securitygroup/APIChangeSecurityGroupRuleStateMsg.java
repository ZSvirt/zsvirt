package org.zstack.network.securitygroup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@RestRequest(
        path = "/security-groups/{securityGroupUuid}/rules/state/actions",
        method = HttpMethod.PUT,
        responseClass = APIChangeSecurityGroupRuleStateEvent.class,
        isAction = true
)
public class APIChangeSecurityGroupRuleStateMsg extends APIMessage implements SecurityGroupMessage {
    @APIParam(required = true, nonempty = true)
    private String securityGroupUuid;

    @APIParam(required = true, nonempty = true)
    private List<String> ruleUuids;

	@APIParam(required = true, validValues = {"Enabled", "Disabled"})
	private String state;

	@Override
    public String getSecurityGroupUuid() {
        return securityGroupUuid;
    }
    public void setSecurityGroupUuid(String securityGroupUuid) {
        this.securityGroupUuid = securityGroupUuid;
	}

	public List<String> getRuleUuids() {
		return ruleUuids;
	}
	public void setRuleUuids(List<String> ruleUuids) {
		this.ruleUuids = ruleUuids;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public static APIChangeSecurityGroupRuleStateMsg __example__() {
		APIChangeSecurityGroupRuleStateMsg msg = new APIChangeSecurityGroupRuleStateMsg();
		msg.setSecurityGroupUuid(uuid());
		msg.setRuleUuids(asList(uuid()));
		msg.setState("Enabled");
		return msg;
	}
}
