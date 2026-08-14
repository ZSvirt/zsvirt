package org.zstack.vpc;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;

/**
 * Created by weiwang on 20/11/2017
 */
@RestRequest(
        path = "/vpc/virtual-routers/{uuid}/attachable-vpc-l3s",
        method = HttpMethod.POST,
        responseClass = APIGetAttachableVpcL3NetworkReply.class,
        parameterName = "params"
)
public class APIGetAttachableVpcL3NetworkMsg extends APISyncCallMessage {
    @APIParam(resourceType = VirtualRouterVmVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIGetAttachableVpcL3NetworkMsg __example__() {
        APIGetAttachableVpcL3NetworkMsg msg = new APIGetAttachableVpcL3NetworkMsg();
        msg.setUuid(uuid());

        return msg;
    }
}
