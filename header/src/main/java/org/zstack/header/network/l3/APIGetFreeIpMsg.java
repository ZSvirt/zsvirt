package org.zstack.header.network.l3;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.rest.SDK;

/**
 * Created by frank on 6/15/2015.
 */
@RestRequest(
        path = "null",
        optionalPaths = {
                "/l3-networks/ip/free",
                "/l3-networks/{l3NetworkUuid}/ip/free",
                "/l3-networks/ip-ranges/{ipRangeUuid}/ip/free",
        },
        responseClass = APIGetFreeIpReply.class,
        method = HttpMethod.GET
)
@SDK(
        actionsMapping = {
                "GetFreeIp=/l3-networks/ip/free",
                "GetFreeIpOfL3Network=/l3-networks/{l3NetworkUuid}/ip/free",
                "GetFreeIpOfIpRange=/l3-networks/ip-ranges/{ipRangeUuid}/ip/free"
        }
)
public class APIGetFreeIpMsg extends APISyncCallMessage implements L3NetworkMessage {
    @APIParam(resourceType = L3NetworkVO.class, required = false)
    private String l3NetworkUuid;
    @APIParam(resourceType = IpRangeVO.class, required = false)
    private String ipRangeUuid;
    @APIParam(required = false)
    private String start;
    @APIParam(required = false, validValues = {"Normal", "AddressPool"})
    private String ipRangeType;
    @APIParam(required = false, validValues = {"4", "6"})
    private Integer ipVersion;

    private int limit = 100;

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getIpRangeUuid() {
        return ipRangeUuid;
    }

    public void setIpRangeUuid(String ipRangeUuid) {
        this.ipRangeUuid = ipRangeUuid;
    }

    public void setStartIp(String start) {
        this.start = start;
    }

    public String getStart() {
        return start;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public String getIpRangeType() {
        return ipRangeType;
    }

    public void setIpRangeType(String ipRangeType) {
        this.ipRangeType = ipRangeType;
    }

    public Integer getIpVersion() {
        return ipVersion;
    }

    public void setIpVersion(Integer ipVersion) {
        this.ipVersion = ipVersion;
    }

    public static APIGetFreeIpMsg __example__() {
        APIGetFreeIpMsg msg = new APIGetFreeIpMsg();
        msg.setL3NetworkUuid(uuid());
        msg.ipRangeUuid = uuid();
        return msg;
    }
}
