package org.zstack.network.service.virtualrouter;

import org.springframework.http.HttpMethod;
import org.zstack.appliancevm.APIQueryApplianceVmMsg;
import org.zstack.appliancevm.APIQueryApplianceVmReply;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 */
@AutoQuery(replyClass = APIQueryVirtualRouterVmReply.class, inventoryClass = VirtualRouterVmInventory.class)
@RestRequest(
        path = "/vm-instances/appliances/virtual-routers",
        optionalPaths = {"/vm-instances/appliances/virtual-routers/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryApplianceVmReply.class
)
public class APIQueryVirtualRouterVmMsg extends APIQueryApplianceVmMsg {

    public static List<String> __example__() {
        return asList("name=Test-Router");
    }

}
