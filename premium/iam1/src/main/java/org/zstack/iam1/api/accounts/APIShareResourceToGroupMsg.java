package org.zstack.iam1.api.accounts;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vo.ResourceVO;
import org.zstack.iam1.entity.accounts.AccountGroupVO;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestRequest(
        path = "/account-groups/resources/actions",
        method = HttpMethod.PUT,
        responseClass = APIShareResourceToGroupEvent.class,
        isAction = true
)
public class APIShareResourceToGroupMsg extends APIMessage implements AccountGroupMessage {
    @APIParam(resourceType = ResourceVO.class, nonempty = true, scope = APIParam.SCOPE_MUST_OWNER)
    private List<String> resourceUuids;
    @APIParam(resourceType = AccountGroupVO.class)
    private String groupUuid;

    @Override
    public String getAccountGroupUuid() {
        return getGroupUuid();
    }

    public List<String> getResourceUuids() {
        return resourceUuids;
    }

    public void setResourceUuids(List<String> resourceUuids) {
        this.resourceUuids = resourceUuids;
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }

    public static APIShareResourceToGroupMsg __example__() {
        APIShareResourceToGroupMsg msg = new APIShareResourceToGroupMsg();
        msg.setResourceUuids(list(uuid()));
        msg.setGroupUuid(uuid());
        return msg;
    }
}
