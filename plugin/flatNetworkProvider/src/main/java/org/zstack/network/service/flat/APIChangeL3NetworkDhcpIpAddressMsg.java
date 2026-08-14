package org.zstack.network.service.flat;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l3.L3NetworkConstant;
import org.zstack.header.network.l3.L3NetworkMessage;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.rest.RestRequest;

@Action(category = L3NetworkConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/l3-networks/{l3NetworkUuid}/dhcp-ip",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIChangeL3NetworkDhcpIpAddressEvent.class
)
public class APIChangeL3NetworkDhcpIpAddressMsg extends APIMessage implements L3NetworkMessage {
    @APIParam(resourceType = L3NetworkVO.class, checkAccount = true, operationTarget = true)
    private String l3NetworkUuid;
    @APIParam(required = false, nonempty = true)
    private String dhcpServerIp;
    @APIParam(required = false, nonempty = true)
    private String dhcpv6ServerIp;

    @Override
    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getDhcpServerIp() {
        return dhcpServerIp;
    }

    public void setDhcpServerIp(String dhcpServerIp) {
        this.dhcpServerIp = dhcpServerIp;
    }

    public String getDhcpv6ServerIp() {
        return dhcpv6ServerIp;
    }

    public void setDhcpv6ServerIp(String dhcpv6ServerIp) {
        this.dhcpv6ServerIp = dhcpv6ServerIp;
    }

    public static APIChangeL3NetworkDhcpIpAddressMsg __example__() {
        APIChangeL3NetworkDhcpIpAddressMsg msg = new APIChangeL3NetworkDhcpIpAddressMsg();

        msg.setL3NetworkUuid(uuid());
        msg.setDhcpServerIp("192.168.1.100");
        msg.setDhcpv6ServerIp("2024:04:28:01::100");

        return msg;
    }

}
