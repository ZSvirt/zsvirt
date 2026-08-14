package org.zstack.vpc;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.service.VirtualRouterHaAPIMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vpc.VpcMessage;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;

/**
 * Created by weiwang on 20/11/2017
 */
@RestRequest(
        path = "/vpc/virtual-routers/{uuid}/distributed-routing",
        method = HttpMethod.POST,
        responseClass = APISetVpcVRouterDistributedRoutingEnabledEvent.class,
        parameterName = "params"
)
public class APISetVpcVRouterDistributedRoutingEnabledMsg extends APIMessage implements VpcMessage, VirtualRouterHaAPIMessage {
    @APIParam(resourceType = VirtualRouterVmVO.class)
    private String uuid;

    @APIParam(validValues = {"enable","disable"})
    private String stateEvent;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getStateEvent() {
        return stateEvent;
    }

    public void setStateEvent(String stateEvent) {
        this.stateEvent = stateEvent;
    }

    @Override
    public String getVpcRouterUuid() {
        return uuid;
    }

    @Override
    public String getVirtualRouterUuid() {
        return uuid;
    }

    @Override
    public void setVirtualRouterUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APISetVpcVRouterDistributedRoutingEnabledMsg __example__() {
        APISetVpcVRouterDistributedRoutingEnabledMsg msg = new APISetVpcVRouterDistributedRoutingEnabledMsg();
        msg.setUuid(uuid());
        msg.setStateEvent("enable");

        return msg;
    }
}
