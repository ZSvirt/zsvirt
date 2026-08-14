package org.zstack.header.network.l3;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 1/21/2016.
 */
@RestRequest(
        path = "/l3-networks/{l3NetworkUuid}/ip/{ip}/availability",
        method = HttpMethod.GET,
        responseClass = APICheckIpAvailabilityReply.class
)
public class APICheckIpAvailabilityMsg extends APISyncCallMessage implements L3NetworkMessage {
    @APIParam(resourceType = L3NetworkVO.class)
    private String l3NetworkUuid;
    @APIParam(maxLength = 255)
    private String ip;
    @APIParam(required = false)
    private Boolean arpCheck = false;
    @APIParam(required = false)
    private Boolean ipRangeCheck= true;

    @Override
    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Boolean getArpCheck() {
        return arpCheck;
    }

    public void setArpCheck(Boolean arpCheck) {
        this.arpCheck = arpCheck;
    }

    public Boolean getIpRangeCheck() {
        return ipRangeCheck;
    }

    public void setIpRangeCheck(Boolean ipRangeCheck) {
        this.ipRangeCheck = ipRangeCheck;
    }

    public static APICheckIpAvailabilityMsg __example__() {
        APICheckIpAvailabilityMsg msg = new APICheckIpAvailabilityMsg();

        msg.setL3NetworkUuid(uuid());
        msg.setIp("192.168.10.100");

        return msg;
    }

}
