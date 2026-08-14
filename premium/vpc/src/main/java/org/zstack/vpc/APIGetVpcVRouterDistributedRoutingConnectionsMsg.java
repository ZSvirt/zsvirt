package org.zstack.vpc;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vpc.VpcMessage;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;

/**
 * Created by weiwang on 20/11/2017
 */
@RestRequest(
        path = "/vpc/virtual-routers/{uuid}/tracked-connections",
        method = HttpMethod.GET,
        responseClass = APIGetVpcVRouterDistributedRoutingConnectionsReply.class
)
public class APIGetVpcVRouterDistributedRoutingConnectionsMsg extends APISyncCallMessage implements VpcMessage {
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

    public static APIGetVpcVRouterDistributedRoutingConnectionsMsg __example__() {
        APIGetVpcVRouterDistributedRoutingConnectionsMsg msg = new APIGetVpcVRouterDistributedRoutingConnectionsMsg();
        msg.setUuid(uuid());

        return msg;
    }
}
