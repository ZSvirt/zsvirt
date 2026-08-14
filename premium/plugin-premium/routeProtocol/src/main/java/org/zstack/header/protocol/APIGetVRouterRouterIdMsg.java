package org.zstack.header.protocol;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceMessage;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;

@RestRequest(
        path = "/routerArea/{vRouterUuid}/routerid",
        method = HttpMethod.GET,
        responseClass = APIGetVRouterRouterIdReply.class
)
public class APIGetVRouterRouterIdMsg extends APISyncCallMessage implements VmInstanceMessage {
    @APIParam(resourceType = VirtualRouterVmVO.class)
    private String vRouterUuid;

    public String getvRouterUuid() {
        return vRouterUuid;
    }

    public void setvRouterUuid(String vRouterUuid) {
        this.vRouterUuid = vRouterUuid;
    }

    public static APIGetVRouterRouterIdMsg __example__() {
        APIGetVRouterRouterIdMsg msg = new APIGetVRouterRouterIdMsg();
        msg.setvRouterUuid(uuid());

        return msg;
    }

    @Override
    public String getVmInstanceUuid() {
        return vRouterUuid;
    }
}
