package org.zstack.header.protocol;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIGetMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vpc.VpcMessage;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;


@RestRequest(
        path = "/vpc/virtual-routers/{uuid}/attached-ospf",
        method = HttpMethod.POST,
        responseClass = APIGetVpcAttachedOspfReply.class,
        parameterName = "params"
)
public class APIGetVpcAttachedOspfMsg extends APIGetMessage implements VpcMessage{
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

    public static APIGetVpcAttachedOspfMsg __example__() {
        APIGetVpcAttachedOspfMsg msg = new APIGetVpcAttachedOspfMsg();
        msg.setUuid(uuid());

        return msg;
    }
}

