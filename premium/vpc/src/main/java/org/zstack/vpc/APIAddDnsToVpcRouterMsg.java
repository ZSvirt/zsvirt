package org.zstack.vpc;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.service.VirtualRouterHaAPIMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vpc.VpcMessage;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;

@RestRequest(
        path = "/vpc/virtual-routers/{uuid}/dns",
        method = HttpMethod.POST,
        responseClass = APIAddDnsToVpcRouterEvent.class,
        parameterName = "params"
)
public class APIAddDnsToVpcRouterMsg extends APICreateMessage implements VpcMessage, VirtualRouterHaAPIMessage {
    /**
     * @desc VpcRouter uuid
     */
    @APIParam(resourceType = VirtualRouterVmVO.class)
    private String uuid;
    /**
     * @desc dns in IPv4
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

    public static APIAddDnsToVpcRouterMsg __example__() {
        APIAddDnsToVpcRouterMsg msg = new APIAddDnsToVpcRouterMsg();
        msg.setUuid(uuid());
        msg.setDns("8.8.8.8");

        return msg;
    }
}
