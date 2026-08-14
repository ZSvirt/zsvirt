package org.zstack.header.protocol;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryVRouterOspfAreaReply.class, inventoryClass = RouterAreaInventory.class)
@RestRequest(
        path = "/routerArea",
        optionalPaths = {"/routerArea/{uuid}"},
        responseClass = APIQueryVRouterOspfAreaReply.class,
        method = HttpMethod.GET
)
public class APIQueryVRouterOspfAreaMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("areaId=1.1.1.1");
    }
}
