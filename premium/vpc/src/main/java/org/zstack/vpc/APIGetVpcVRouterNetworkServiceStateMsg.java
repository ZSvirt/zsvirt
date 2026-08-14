package org.zstack.vpc;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vpc.VpcMessage;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;

/**
 * created by zhanyong.miao on 23/02/2019
 */
@RestRequest(
        path = "/vpc/virtual-routers/{uuid}/networkservicestate",
        method = HttpMethod.GET,
        responseClass = APIGetVpcVRouterNetworkServiceStateReply.class
)
public class APIGetVpcVRouterNetworkServiceStateMsg extends APISyncCallMessage implements VpcMessage {
    @APIParam(resourceType = VirtualRouterVmVO.class)
    private String uuid;

    @APIParam(validValues = {"SNAT"})
    private String networkService;

    @APIParam(required = false,resourceType = L3NetworkVO.class)
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

    @Override
    public String getVpcRouterUuid() {
        return uuid;
    }

    public static APIGetVpcVRouterNetworkServiceStateMsg __example__() {
        APIGetVpcVRouterNetworkServiceStateMsg msg = new APIGetVpcVRouterNetworkServiceStateMsg();
        msg.setUuid(uuid());
        msg.setNetworkService("SNAT");

        return msg;
    }
}
