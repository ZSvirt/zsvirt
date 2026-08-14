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
        path = "/vpc/virtual-routers/{uuid}/attached-eip",
        method = HttpMethod.POST,
        responseClass = APIGetVpcAttachedEipReply.class,
        parameterName = "params"
)
public class APIGetVpcAttachedEipMsg extends APIGetMessage implements VpcMessage {
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

    public static APIGetVpcAttachedEipMsg __example__() {
        APIGetVpcAttachedEipMsg msg = new APIGetVpcAttachedEipMsg();
        msg.setUuid(uuid());

        return msg;
    }
}
