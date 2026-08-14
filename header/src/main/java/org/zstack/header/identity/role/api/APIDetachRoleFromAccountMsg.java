package org.zstack.header.identity.role.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(path = "/identities/accounts/{accountUuid}/roles/{roleUuid}", method = HttpMethod.DELETE, responseClass = APIDetachRoleFromAccountEvent.class)
public class APIDetachRoleFromAccountMsg extends APIDeleteMessage implements RoleMessage {
    @APIParam
    private String roleUuid;
    @APIParam
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

    public static APIDetachRoleFromAccountMsg __example__() {
        APIDetachRoleFromAccountMsg msg = new APIDetachRoleFromAccountMsg();

        msg.setRoleUuid(uuid());
        msg.setAccountUuid(uuid());

        return msg;
    }
}
