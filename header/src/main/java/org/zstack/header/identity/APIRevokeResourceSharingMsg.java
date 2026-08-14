package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vo.ResourceVO;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 7/13/2015.
 */
@RestRequest(
        path = "/accounts/resources/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIRevokeResourceSharingEvent.class
)
public class APIRevokeResourceSharingMsg extends APIMessage {
    @APIParam(resourceType = ResourceVO.class, nonempty = true, scope = APIParam.SCOPE_MUST_OWNER)
    private List<String> resourceUuids;
    private boolean toPublic;
    @APIParam(resourceType = AccountVO.class, required = false)
    private List<String> accountUuids;
    private boolean all;

    public boolean isAll() {
        return all;
    }

    public void setAll(boolean all) {
        this.all = all;
    }

    public List<String> getResourceUuids() {
        return resourceUuids;
    }

    public void setResourceUuids(List<String> resourceUuids) {
        this.resourceUuids = resourceUuids;
    }

    public boolean isToPublic() {
        return toPublic;
    }

    public void setToPublic(boolean toPublic) {
        this.toPublic = toPublic;
    }

    public List<String> getAccountUuids() {
        return accountUuids;
    }

    public void setAccountUuids(List<String> accountUuids) {
        this.accountUuids = accountUuids;
    }

    public static APIRevokeResourceSharingMsg __example__() {
        APIRevokeResourceSharingMsg msg = new APIRevokeResourceSharingMsg();
        msg.setAccountUuids(list(uuid(), uuid()));
        msg.setResourceUuids(list(uuid(), uuid()));
        msg.setToPublic(false);
        msg.setAll(false);
        return msg;
    }
}
