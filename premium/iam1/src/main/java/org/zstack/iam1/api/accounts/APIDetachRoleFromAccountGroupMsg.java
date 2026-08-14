package org.zstack.iam1.api.accounts;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.role.RoleVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.iam1.entity.accounts.AccountGroupVO;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestRequest(
        path = "/account-groups/{groupUuid}/roles",
        method = HttpMethod.DELETE,
        responseClass = APIDetachRoleFromAccountGroupEvent.class
)
public class APIDetachRoleFromAccountGroupMsg extends APIMessage implements AccountGroupMessage {
    @APIParam(resourceType = AccountGroupVO.class)
    private String groupUuid;
    @APIParam(resourceType = RoleVO.class, nonempty = true)
    private List<String> roleUuids;

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

    public List<String> getRoleUuids() {
        return roleUuids;
    }

    public void setRoleUuids(List<String> roleUuids) {
        this.roleUuids = roleUuids;
    }

    public static APIDetachRoleFromAccountGroupMsg __example__() {
        APIDetachRoleFromAccountGroupMsg msg = new APIDetachRoleFromAccountGroupMsg();
        msg.setGroupUuid(uuid());
        msg.setRoleUuids(list(uuid()));
        return msg;
    }
}
