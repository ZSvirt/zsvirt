package org.zstack.header.identity.role.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.role.RolePolicyStatement;
import org.zstack.header.identity.role.RoleVO;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

import java.util.List;

@RestRequest(path = "/identities/roles", method = HttpMethod.POST, responseClass = APICreateRoleEvent.class, parameterName = "params")
public class APICreateRoleMsg extends APICreateMessage {
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description = "";
    @APIParam(required = false)
    private List<Object> policies;
    @APIParam(required = false, resourceType = RoleVO.class, scope = APIParam.SCOPE_ALLOWED_ALL)
    private String baseOnRole;
    @APINoSee
    private List<RolePolicyStatement> formatPolicies;

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

    public List<Object> getPolicies() {
        return policies;
    }

    public void setPolicies(List<Object> policies) {
        this.policies = policies;
    }

    public String getBaseOnRole() {
        return baseOnRole;
    }

    public void setBaseOnRole(String baseOnRole) {
        this.baseOnRole = baseOnRole;
    }

    public List<RolePolicyStatement> getFormatPolicies() {
        return formatPolicies;
    }

    public void setFormatPolicies(List<RolePolicyStatement> formatPolicies) {
        this.formatPolicies = formatPolicies;
    }

    public static APICreateRoleMsg __example__() {
        APICreateRoleMsg msg = new APICreateRoleMsg();
        msg.setName("role-1");
        msg.setDescription("role for test");

        return msg;
    }
}
