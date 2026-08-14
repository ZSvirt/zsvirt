package org.zstack.iam1.api.accounts;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.iam1.entity.accounts.AccountGroupVO;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestRequest(
        path = "/account-groups/{groupUuid}/accounts",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAddAccountToGroupEvent.class
)
public class APIAddAccountToGroupMsg extends APIMessage implements AccountGroupMessage {
    @APIParam(resourceType = AccountGroupVO.class)
    private String groupUuid;
    @APIParam(resourceType = AccountVO.class, nonempty = true, scope = APIParam.SCOPE_ALLOWED_ALL)
    private List<String> accountUuids;

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

    public List<String> getAccountUuids() {
        return accountUuids;
    }

    public void setAccountUuids(List<String> accountUuids) {
        this.accountUuids = accountUuids;
    }

    public static APIAddAccountToGroupMsg __example__() {
        APIAddAccountToGroupMsg msg = new APIAddAccountToGroupMsg();
        msg.setGroupUuid(uuid());
        msg.setAccountUuids(list(uuid()));
        return msg;
    }
}
