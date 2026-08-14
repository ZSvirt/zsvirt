package org.zstack.iam1.api.accounts;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.iam1.entity.accounts.AccountGroupVO;

@RestRequest(
        path = "/account-groups/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIMoveAccountGroupEvent.class
)
public class APIMoveAccountGroupMsg extends APIMessage implements AccountGroupMessage {
    @APIParam(resourceType = AccountGroupVO.class)
    private String uuid;
    @APIParam(resourceType = AccountGroupVO.class, required = false)
    private String parentUuid;

    @Override
    public String getAccountGroupUuid() {
        return getUuid();
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getParentUuid() {
        return parentUuid;
    }

    public void setParentUuid(String parentUuid) {
        this.parentUuid = parentUuid;
    }

    public static APIMoveAccountGroupMsg __example__() {
        APIMoveAccountGroupMsg msg = new APIMoveAccountGroupMsg();
        msg.setUuid(uuid());
        msg.setParentUuid(uuid());
        return msg;
    }
}
