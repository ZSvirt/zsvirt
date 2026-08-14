package org.zstack.vrouterRoute;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.network.securitygroup.APIQuerySecurityGroupReply;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by weiwang on 15/06/2017.
 */
@AutoQuery(replyClass = APIQueryVRouterRouteTableReply.class, inventoryClass = VRouterRouteTableInventory.class)
@RestRequest(
        path = "/vrouter-route-tables",
        optionalPaths = {"/vrouter-route-tables/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryVRouterRouteTableReply.class
)
public class APIQueryVRouterRouteTableMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList();
    }
}
