package org.zstack.header.network.l3;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

@TagResourceType(L3NetworkVO.class)
@Action(category = L3NetworkConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/l3-networks/{l3NetworkUuid}/reserved-ip-ranges",
        method = HttpMethod.POST,
        responseClass = APIAddReservedIpRangeEvent.class,
        parameterName = "params"
)
public class APIAddReservedIpRangeMsg extends APICreateMessage implements L3NetworkMessage, APIAuditor {
    /**
     * @desc l3Network uuid
     */
    @APIParam(resourceType = L3NetworkVO.class, checkAccount = true, operationTarget = true)
    private String l3NetworkUuid;
    /**
     * @desc start IP address (IPv4 or IPv6)
     */
    @APIParam
    private String startIp;
    /**
     * @desc end IP address (IPv4 or IPv6)
     */
    @APIParam
    private String endIp;

    @Override
    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
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

    public static APIAddReservedIpRangeMsg __example__() {
        APIAddReservedIpRangeMsg msg = new APIAddReservedIpRangeMsg();

        msg.setL3NetworkUuid(uuid());
        msg.setStartIp("192.168.100.10");
        msg.setEndIp("192.168.100.250");

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APIAddReservedIpRangeEvent)rsp).getInventory().getL3NetworkUuid() : "", L3NetworkVO.class);
    }
}
