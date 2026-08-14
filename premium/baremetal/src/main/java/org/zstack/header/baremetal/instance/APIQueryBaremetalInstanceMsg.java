package org.zstack.header.baremetal.instance;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;
/**
 * Created by GuoYi on 7/4/18.
 */
@AutoQuery(replyClass = APIQueryBaremetalInstanceReply.class, inventoryClass = BaremetalInstanceInventory.class)
@RestRequest(
        path = "/baremetal/instances",
        optionalPaths = {"/baremetal/instances/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryBaremetalInstanceReply.class
)
public class APIQueryBaremetalInstanceMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("uuid = " + uuid());
    }
}
