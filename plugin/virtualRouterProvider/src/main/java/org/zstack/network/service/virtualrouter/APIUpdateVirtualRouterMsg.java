package org.zstack.network.service.virtualrouter;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceMessage;

/**
 * Created by shixin.ruan 2020/02/12.
 */
@RestRequest(
        path = "/vm-instances/appliances/virtual-routers/{vmInstanceUuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateVirtualRouterEvent.class
)
public class APIUpdateVirtualRouterMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VirtualRouterVmVO.class)
    private String vmInstanceUuid;
    @APIParam(resourceType = L3NetworkVO.class, required = false)
    private String defaultRouteL3NetworkUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getDefaultRouteL3NetworkUuid() {
        return defaultRouteL3NetworkUuid;
    }

    public void setDefaultRouteL3NetworkUuid(String defaultRouteL3NetworkUuid) {
        this.defaultRouteL3NetworkUuid = defaultRouteL3NetworkUuid;
    }

    public static APIUpdateVirtualRouterMsg __example__() {
        APIUpdateVirtualRouterMsg msg = new APIUpdateVirtualRouterMsg();
        msg.vmInstanceUuid = uuid();
        msg.defaultRouteL3NetworkUuid = uuid();
        return msg;
    }
}
