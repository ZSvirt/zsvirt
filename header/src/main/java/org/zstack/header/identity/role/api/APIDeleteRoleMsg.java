package org.zstack.header.identity.role.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.role.RoleVO;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(path = "/identities/roles/{uuid}", method = HttpMethod.DELETE, responseClass = APIDeleteRoleEvent.class)
public class APIDeleteRoleMsg extends APIDeleteMessage implements RoleMessage {
    @APIParam(resourceType = RoleVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getRoleUuid() {
        return uuid;
    }

    public static APIDeleteRoleMsg __example__() {
        APIDeleteRoleMsg msg = new APIDeleteRoleMsg();
        msg.setUuid(uuid());

        return msg;
    }
}
