package org.zstack.vpc;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.service.VirtualRouterHaAPIMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vpc.VpcMessage;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;

/**
 * created by zhanyong.miao on 23/02/2019
 */
@RestRequest(
        path = "/vpc/virtual-routers/{uuid}/networkservicestate",
        method = HttpMethod.POST,
        responseClass = APISetVpcVRouterNetworkServiceStateEvent.class,
        parameterName = "params"
)
public class APISetVpcVRouterNetworkServiceStateMsg extends APIMessage implements VpcMessage, VirtualRouterHaAPIMessage {
    @APIParam(resourceType = VirtualRouterVmVO.class)
    private String uuid;

    @APIParam(validValues = {"SNAT"})
    private String networkService;

    @APIParam(validValues = {"enable", "disable"})
    private String state;

    @APIParam(required = false, resourceType = L3NetworkVO.class)
    private String l3NetworkUuid;

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getNetworkService() {
        return networkService;
    }

    public void setNetworkService(String networkService) {
        this.networkService = networkService;
    }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    @Override
    public String getVirtualRouterUuid() {
        return uuid;
    }

    @Override
    public void setVirtualRouterUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getVpcRouterUuid() {
        return uuid;
    }

    public static APISetVpcVRouterNetworkServiceStateMsg __example__() {
        APISetVpcVRouterNetworkServiceStateMsg msg = new APISetVpcVRouterNetworkServiceStateMsg();
        msg.setUuid(uuid());
        msg.setNetworkService("SNAT");
        msg.setState("enable");
        return msg;
    }
}
