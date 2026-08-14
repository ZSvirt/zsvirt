package org.zstack.header.network.l3;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

/**
 */
@TagResourceType(L3NetworkVO.class)
@RestRequest(
        path = "/l3-networks/{l3NetworkUuid}/ip-ranges/by-cidr",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAddIpRangeByNetworkCidrEvent.class
)
public class APIAddIpRangeByNetworkCidrMsg extends APICreateMessage implements L3NetworkMessage {
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(required = false, maxLength = 2048)
    private String description;
    @APIParam(resourceType = L3NetworkVO.class)
    private String l3NetworkUuid;
    @APIParam
    private String networkCidr;
    @APIParam(required = false, maxLength = 64)
    private String gateway;

    /**
     * @desc IPv4 range type
     */
    @APIParam(required = false, validValues = {"Normal", "AddressPool"})
    private String ipRangeType;

    @Override
    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getNetworkCidr() {
        return networkCidr;
    }

    public void setNetworkCidr(String networkCidr) {
        this.networkCidr = networkCidr;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getIpRangeType() {
        return ipRangeType;
    }

    public void setIpRangeType(String ipRangeType) {
        this.ipRangeType = ipRangeType;
    }

    public static APIAddIpRangeByNetworkCidrMsg __example__() {
        APIAddIpRangeByNetworkCidrMsg msg = new APIAddIpRangeByNetworkCidrMsg();

        msg.setName("Test-IPRange");
        msg.setL3NetworkUuid(uuid());
        msg.setNetworkCidr("192.168.10.0/24");
        msg.setIpRangeType(IpRangeType.Normal.toString());

        return msg;
    }
}
