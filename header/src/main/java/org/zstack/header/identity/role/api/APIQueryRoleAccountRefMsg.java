package org.zstack.header.identity.role.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.role.RoleAccountRefInventory;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@AutoQuery(replyClass = APIQueryRoleAccountRefReply.class, inventoryClass = RoleAccountRefInventory.class)
@RestRequest(
        path = "/identities/role-account-refs",
        method = HttpMethod.GET,
        responseClass = APIQueryRoleAccountRefReply.class
)
public class APIQueryRoleAccountRefMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return list("roleUuid=686cb963323e491e955a0fd0b49dd743");
    }
}
