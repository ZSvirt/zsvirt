package org.zstack.header.network.l2;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryL2NetworkReply.class, inventoryClass = L2NetworkInventory.class)
@RestRequest(
        path = "/l2-networks",
        optionalPaths = {"/l2-networks/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryL2NetworkReply.class
)
public class APIQueryL2NetworkMsg extends APIQueryMessage {


    public static List<String> __example__() {
        return asList();
    }

}
