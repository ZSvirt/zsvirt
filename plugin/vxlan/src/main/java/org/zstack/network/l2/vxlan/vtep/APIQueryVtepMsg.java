package org.zstack.network.l2.vxlan.vtep;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by weiwang on 27/05/2017.
 */
@AutoQuery(replyClass = APIQueryVtepReply.class, inventoryClass = VtepInventory.class)
@RestRequest(
        path = "/l2-networks/vteps",
        optionalPaths = {"/l2-networks/vteps/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryVtepReply.class
)
public class APIQueryVtepMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList();
    }
}
