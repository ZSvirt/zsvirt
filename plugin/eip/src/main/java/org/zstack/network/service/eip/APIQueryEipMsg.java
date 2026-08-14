package org.zstack.network.service.eip;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 */
@AutoQuery(replyClass = APIQueryEipReply.class, inventoryClass = EipInventory.class)
@RestRequest(
        path = "/eips",
        optionalPaths = {"/eips/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryEipReply.class
)
public class APIQueryEipMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList();
    }

}
