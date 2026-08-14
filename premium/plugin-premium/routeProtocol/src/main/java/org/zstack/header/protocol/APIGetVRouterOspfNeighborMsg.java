package org.zstack.header.protocol;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceMessage;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;

@RestRequest(
        path = "/routerArea/{vRouterUuid}/neighbor",
        method = HttpMethod.GET,
        responseClass = APIGetVRouterOspfNeighborReply.class
)
public class APIGetVRouterOspfNeighborMsg  extends APISyncCallMessage implements VmInstanceMessage {
    @APIParam(resourceType = VirtualRouterVmVO.class)
    private String vRouterUuid;

    public static APIGetVRouterOspfNeighborMsg __example__() {
        APIGetVRouterOspfNeighborMsg msg = new APIGetVRouterOspfNeighborMsg();
        msg.setvRouterUuid(uuid());
        return msg;
    }

    public String getvRouterUuid() {
        return vRouterUuid;
    }

    public void setvRouterUuid(String vRouterUuid) {
        this.vRouterUuid = vRouterUuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return vRouterUuid;
    }
}
