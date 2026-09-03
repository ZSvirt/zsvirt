package org.zstack.iam1.api.accounts;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.iam1.entity.accounts.AccountGroupVO;

@RestRequest(
        path = "/account-groups/{groupUuid}/roles",
        method = HttpMethod.GET,
        responseClass = APIGetRolesForAccountGroupReply.class
)
public class APIGetRolesForAccountGroupMsg extends APISyncCallMessage implements AccountGroupMessage {
    @APIParam(resourceType = AccountGroupVO.class)
    private String groupUuid;
    @APIParam(required = false)
    private boolean includeInheritedRoles;

    @Override
    public String getAccountGroupUuid() {
        return getGroupUuid();
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }

    public boolean isIncludeInheritedRoles() {
        return includeInheritedRoles;
    }

    public void setIncludeInheritedRoles(boolean includeInheritedRoles) {
        this.includeInheritedRoles = includeInheritedRoles;
    }

    public static APIGetRolesForAccountGroupMsg __example__() {
        APIGetRolesForAccountGroupMsg msg = new APIGetRolesForAccountGroupMsg();
        msg.setGroupUuid(uuid());
        return msg;
    }
}
