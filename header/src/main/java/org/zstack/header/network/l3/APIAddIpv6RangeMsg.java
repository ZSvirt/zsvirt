package org.zstack.header.network.l3;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;
import org.zstack.utils.network.IPv6Constants;

@TagResourceType(L3NetworkVO.class)
@RestRequest(
        path = "/l3-networks/{l3NetworkUuid}/ipv6-ranges",
        method = HttpMethod.POST,
        responseClass = APIAddIpRangeEvent.class,
        parameterName = "params"
)
public class APIAddIpv6RangeMsg extends APICreateMessage implements L3NetworkMessage, APIAuditor {
    /**
     * @desc l3Network uuid
     */
    @APIParam(resourceType = L3NetworkVO.class)
    private String l3NetworkUuid;
    /**
     * @desc max length of 255 characters
     */
    @APIParam(maxLength = 255)
    private String name;
    /**
     * @desc max length of 2048 characters
     */
    @APIParam(required = false, maxLength = 2048)
    private String description;
    /**
     * @desc start IPv6 address
     */
    @APIParam
    private String startIp;
    /**
     * @desc end IPv6 address
     */
    @APIParam
    private String endIp;

    /**
     * @desc IPv6 gateway
     */
    @APIParam
    private String gateway;
    
    /**
     * @desc IPv6 prefixLen
     */
    @APIParam(numberRange = {8, 126})
    private Integer prefixLen;

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

    public String getStartIp() {
        return startIp;
    }

    public void setStartIp(String startIp) {
        this.startIp = startIp;
    }

    public String getEndIp() {
        return endIp;
    }

    public void setEndIp(String endIP) {
        this.endIp = endIP;
    }

    public Integer getPrefixLen() {
        return prefixLen;
    }

    public void setPrefixLen(Integer prefixLen) {
        this.prefixLen = prefixLen;
    }

    public String getAddressMode() {
        return addressMode;
    }

    public void setAddressMode(String addressMode) {
        this.addressMode = addressMode;
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

    public static APIAddIpv6RangeMsg __example__() {
        APIAddIpv6RangeMsg msg = new APIAddIpv6RangeMsg();

        msg.setL3NetworkUuid(uuid());
        msg.setName("Test-IP-Range");
        msg.setStartIp("2002:2001::02");
        msg.setEndIp("2002:2001::FE");
        msg.setGateway("2002:2001::01");
        msg.setPrefixLen(64);
        msg.setAddressMode(IPv6Constants.Stateful_DHCP);
        msg.setIpRangeType(IpRangeType.Normal.toString());

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APIAddIpRangeEvent)rsp).getInventory().getUuid() : "", IpRangeVO.class);
    }
}
