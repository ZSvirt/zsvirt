package org.zstack.vrouterRoute;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by weiwang on 15/06/2017.
 */
@AutoQuery(replyClass = APIQueryVRouterRouteEntryReply.class, inventoryClass = VRouterRouteEntryInventory.class)
@RestRequest(
        path = "/vrouter-route-tables/route-entries",
        optionalPaths = {"/vrouter-route-tables/route-entries/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryVRouterRouteEntryReply.class
)
public class APIQueryVRouterRouteEntryMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList();
    }
}
