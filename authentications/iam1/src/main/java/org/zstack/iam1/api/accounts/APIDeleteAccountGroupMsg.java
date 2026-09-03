package org.zstack.iam1.api.accounts;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.iam1.entity.accounts.AccountGroupVO;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestRequest(
        path = "/account-groups/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteAccountGroupEvent.class
)
public class APIDeleteAccountGroupMsg extends APIDeleteMessage implements AccountGroupMessage {
    @APIParam(resourceType = AccountGroupVO.class, successIfResourceNotExisting = true)
    private String uuid;

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

    @Override
    public List<String> getDeletedResourceUuidList() {
        return list(getUuid());
    }

    public static APIDeleteAccountGroupMsg __example__() {
        APIDeleteAccountGroupMsg msg = new APIDeleteAccountGroupMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
