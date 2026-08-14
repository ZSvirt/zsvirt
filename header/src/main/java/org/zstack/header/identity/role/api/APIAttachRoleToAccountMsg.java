package org.zstack.header.identity.role.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.identity.role.RoleVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/identities/accounts/{accountUuid}/roles/{roleUuid}",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAttachRoleToAccountEvent.class
)
public class APIAttachRoleToAccountMsg extends APIMessage implements RoleMessage {
    @APIParam(resourceType = RoleVO.class)
    private String roleUuid;
    @APIParam(resourceType = AccountVO.class)
    private String accountUuid;

    public String getRoleUuid() {
        return roleUuid;
    }

    public void setRoleUuid(String roleUuid) {
        this.roleUuid = roleUuid;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public static APIAttachRoleToAccountMsg __example__() {
        APIAttachRoleToAccountMsg msg = new APIAttachRoleToAccountMsg();

        msg.setRoleUuid(uuid());
        msg.setAccountUuid(uuid());

        return msg;
    }
}
