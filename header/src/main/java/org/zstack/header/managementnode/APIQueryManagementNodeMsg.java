package org.zstack.header.managementnode;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 */
@AutoQuery(replyClass = APIQueryManagementNodeReply.class, inventoryClass = ManagementNodeInventory.class)
@RestRequest(
        path = "/management-nodes",
        optionalPaths = {"/management-nodes/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryManagementNodeReply.class
)
public class APIQueryManagementNodeMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList("uuid=" + uuid());
    }
}
