package org.zstack.vpc;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vpc.VpcRouterVmInventory;
import org.zstack.network.service.virtualrouter.APIQueryVirtualRouterVmMsg;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryVpcRouterReply.class, inventoryClass = VpcRouterVmInventory.class)
@RestRequest(
        path = "/vpc/virtual-routers",
        optionalPaths = {"/vpc/virtual-routers/{uuid}"},
        responseClass = APIQueryVpcRouterReply.class,
        method = HttpMethod.GET
)
public class APIQueryVpcRouterMsg extends APIQueryVirtualRouterVmMsg {
    public static List<String> __example__() {
        return asList("name=vpcRouter");
    }
}
