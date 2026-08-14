package org.zstack.vrouterRoute;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceMessage;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;

/**
 * Created by weiwang on 15/06/2017.
 */
@RestRequest(
        path = "/vrouter-route-tables/vrouter/{virtualRouterVmUuid}",
        method = HttpMethod.GET,
        responseClass = APIGetVRouterRouteTableReply.class
)
public class APIGetVRouterRouteTableMsg extends APISyncCallMessage implements VmInstanceMessage {
    @APIParam(resourceType = VirtualRouterVmVO.class)
    private String virtualRouterVmUuid;

    public static APIGetVRouterRouteTableMsg __example__() {
        APIGetVRouterRouteTableMsg msg = new APIGetVRouterRouteTableMsg();
        msg.setVirtualRouterVmUuid(uuid());
        return msg;
    }

    public String getVirtualRouterVmUuid() {
        return virtualRouterVmUuid;
    }

    public void setVirtualRouterVmUuid(String virtualRouterVmUuid) {
        this.virtualRouterVmUuid = virtualRouterVmUuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return virtualRouterVmUuid;
    }
}
