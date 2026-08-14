package org.zstack.vrouterRoute;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by weiwang on 17/06/2017.
 */
@AutoQuery(replyClass = APIQueryVirtualRouterVRouterRouteTableRefReply.class, inventoryClass = VirtualRouterVRouterRouteTableRefInventory.class)
@RestRequest(
        path = "/vrouter-route-tables/virtual-router-refs",
        method = HttpMethod.GET,
        responseClass = APIQueryVirtualRouterVRouterRouteTableRefReply.class
)
public class APIQueryVirtualRouterVRouterRouteTableRefMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList();
    }
}
