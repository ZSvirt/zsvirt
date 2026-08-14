package org.zstack.network.service.vip;
import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.message.APIGetMessage;

@RestRequest(
        path = "/vips/{vipUuid}/get-port-availability",
        method = HttpMethod.GET,
        responseClass = APIGetVipAvailablePortReply.class
)
public class APIGetVipAvailablePortMsg extends APIGetMessage {

    @APIParam(resourceType = VipVO.class)
    private String vipUuid;    

    @APIParam(validValues = {"TCP", "UDP"})
    private String protocolType;

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }

    public String getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(String protocolType) {
        this.protocolType = protocolType;
    }
 
    public static APIGetVipAvailablePortMsg __example__() {
        APIGetVipAvailablePortMsg msg = new APIGetVipAvailablePortMsg();
        msg.setVipUuid(uuid());
        msg.setProtocolType("UDP");
        msg.setStart(1);
        msg.setLimit(10);
        return msg;
    }
}
