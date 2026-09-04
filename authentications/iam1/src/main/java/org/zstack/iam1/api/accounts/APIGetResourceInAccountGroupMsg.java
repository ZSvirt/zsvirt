package org.zstack.iam1.api.accounts;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.iam1.entity.accounts.AccountGroupVO;

@RestRequest(
        path = "/account-groups/{groupUuid}/resources",
        method = HttpMethod.GET,
        responseClass = APIGetResourceInAccountGroupReply.class
)
public class APIGetResourceInAccountGroupMsg extends APISyncCallMessage implements AccountGroupMessage {
    @APIParam(resourceType = AccountGroupVO.class)
    private String groupUuid;
    @APIParam(required = false)
    private boolean includeInheritedResources;

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

    public boolean isIncludeInheritedResources() {
        return includeInheritedResources;
    }

    public void setIncludeInheritedResources(boolean includeInheritedResources) {
        this.includeInheritedResources = includeInheritedResources;
    }

    public static APIGetResourceInAccountGroupMsg __example__() {
        APIGetResourceInAccountGroupMsg msg = new APIGetResourceInAccountGroupMsg();
        msg.setGroupUuid(uuid(AccountGroupVO.class));
        return msg;
    }
}
