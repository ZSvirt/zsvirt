package org.zstack.header.network.l3;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;
import org.zstack.utils.network.IPv6Constants;

/**
 */
@TagResourceType(L3NetworkVO.class)
@RestRequest(
        path = "/l3-networks/{l3NetworkUuid}/ipv6-ranges/by-cidr",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAddIpRangeByNetworkCidrEvent.class
)
public class APIAddIpv6RangeByNetworkCidrMsg extends APICreateMessage implements L3NetworkMessage {
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(required = false, maxLength = 2048)
    private String description;
    @APIParam(resourceType = L3NetworkVO.class)
    private String l3NetworkUuid;
    @APIParam
    private String networkCidr;
    @APIParam(validValues = {IPv6Constants.SLAAC, IPv6Constants.Stateful_DHCP, IPv6Constants.Stateless_DHCP})
    private String addressMode;
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

    public String getAddressMode() {
        return addressMode;
    }

    public void setAddressMode(String addressMode) {
        this.addressMode = addressMode;
    }

    public String getIpRangeType() {
        return ipRangeType;
    }

    public void setIpRangeType(String ipRangeType) {
        this.ipRangeType = ipRangeType;
    }

    public static APIAddIpv6RangeByNetworkCidrMsg __example__() {
        APIAddIpv6RangeByNetworkCidrMsg msg = new APIAddIpv6RangeByNetworkCidrMsg();

        msg.setName("Test-IPRange");
        msg.setL3NetworkUuid(uuid());
        msg.setNetworkCidr("2002:2001::/64");
        msg.setAddressMode(IPv6Constants.SLAAC);
        msg.setIpRangeType(IpRangeType.Normal.toString());

        return msg;
    }
}
