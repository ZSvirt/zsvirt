package org.zstack.header.identity.role.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.role.RoleInventory;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@AutoQuery(replyClass = APIQueryRoleReply.class, inventoryClass = RoleInventory.class)
@RestRequest(path = "/identities/roles", optionalPaths = {"/identities/roles/{uuid}"},
        method = HttpMethod.GET, responseClass = APIQueryRoleReply.class)
public class APIQueryRoleMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return list("name=test");
    }
}
