package org.zstack.vpc;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIGetMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vpc.VpcMessage;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;

/**
 * Created by shixin.ruan on 2021/03/23
 */
@RestRequest(
        path = "/vpc/virtual-routers/{uuid}/attached-lb",
        method = HttpMethod.POST,
        responseClass = APIGetVpcAttachedLoadBalancerReply.class,
        parameterName = "params"
)
public class APIGetVpcAttachedLoadBalancerMsg extends APIGetMessage implements VpcMessage {
    @APIParam(resourceType = VirtualRouterVmVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getVpcRouterUuid() {
        return uuid;
    }

    public static APIGetVpcAttachedLoadBalancerMsg __example__() {
        APIGetVpcAttachedLoadBalancerMsg msg = new APIGetVpcAttachedLoadBalancerMsg();
        msg.setUuid(uuid());

        return msg;
    }
}
