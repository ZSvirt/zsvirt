package org.zstack.vpc;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.service.VirtualRouterHaAPIMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vpc.VpcMessage;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;

@RestRequest(
        path = "/vpc/virtual-routers/{uuid}/dns",
        method = HttpMethod.DELETE,
        responseClass = APIRemoveDnsFromVpcRouterEvent.class
)
public class APIRemoveDnsFromVpcRouterMsg extends APIMessage implements VpcMessage, VirtualRouterHaAPIMessage {
    /**
     * @desc l3Network uuid
     */
    @APIParam(resourceType = VirtualRouterVmVO.class)
    private String uuid;
    /**
     * @desc dns ip address
     */
    @APIParam
    private String dns;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getDns() {
        return dns;
    }

    public void setDns(String dns) {
        this.dns = dns;
    }

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

    public static APIRemoveDnsFromVpcRouterMsg __example__() {
        APIRemoveDnsFromVpcRouterMsg msg = new APIRemoveDnsFromVpcRouterMsg();

        msg.setUuid(uuid());
        msg.setDns("8.8.4.4");

        return msg;
    }
}
