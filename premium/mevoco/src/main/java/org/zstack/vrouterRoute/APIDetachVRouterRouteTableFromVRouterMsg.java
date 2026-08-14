package org.zstack.vrouterRoute;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.service.VirtualRouterHaAPIMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceMessage;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;

/**
 * Created by weiwang on 15/06/2017.
 */
@RestRequest(
        path = "/vrouter-route-tables/{routeTableUuid}/detach/{virtualRouterVmUuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDetachVRouterRouteTableFromVRouterEvent.class
)
public class APIDetachVRouterRouteTableFromVRouterMsg extends APIMessage implements VmInstanceMessage, VirtualRouterHaAPIMessage {
    @APIParam(resourceType = VRouterRouteTableVO.class)
    private String routeTableUuid;

    @APIParam(resourceType = VirtualRouterVmVO.class)
    private String virtualRouterVmUuid;

    public static APIDetachVRouterRouteTableFromVRouterMsg __example__() {
        APIDetachVRouterRouteTableFromVRouterMsg msg = new APIDetachVRouterRouteTableFromVRouterMsg();
        msg.setRouteTableUuid(uuid());
        msg.setVirtualRouterVmUuid(uuid());

        return msg;
    }

    public String getRouteTableUuid() {
        return routeTableUuid;
    }

    public void setRouteTableUuid(String routeTableUuid) {
        this.routeTableUuid = routeTableUuid;
    }

    public String getVirtualRouterVmUuid() {
        return virtualRouterVmUuid;
    }

    public void setVirtualRouterVmUuid(String virtualRouterVmUuid) {
        this.virtualRouterVmUuid = virtualRouterVmUuid;
    }

    @Override
    public String getVirtualRouterUuid() {
        return virtualRouterVmUuid;
    }

    @Override
    public void setVirtualRouterUuid(String uuid) {
        this.virtualRouterVmUuid = uuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return virtualRouterVmUuid;
    }
}
