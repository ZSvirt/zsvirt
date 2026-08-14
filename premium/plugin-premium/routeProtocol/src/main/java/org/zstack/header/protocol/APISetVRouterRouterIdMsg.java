package org.zstack.header.protocol;


import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceMessage;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;

@RestRequest(
        path = "/routerArea/{vRouterUuid}/routerid",
        method = HttpMethod.POST,
        responseClass = APISetVRouterRouterIdEvent.class,
        parameterName = "params"
)
public class APISetVRouterRouterIdMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VirtualRouterVmVO.class)
    private String vRouterUuid;

    @APIParam(maxLength = 16, emptyString = false)
    private String routerId;

    public String getvRouterUuid() {
        return vRouterUuid;
    }

    public void setvRouterUuid(String vRouterUuid) {
        this.vRouterUuid = vRouterUuid;
    }

    public String getRouterId() {
        return routerId;
    }

    public void setRouterId(String routerId) {
        this.routerId = routerId;
    }

    @Override
    public String getVmInstanceUuid() {
        return vRouterUuid;
    }

    public static APISetVRouterRouterIdMsg __example__() {
        APISetVRouterRouterIdMsg msg = new APISetVRouterRouterIdMsg();
        msg.setvRouterUuid(uuid());
        msg.setRouterId("10.10.10.1");

        return msg;
    }
}
