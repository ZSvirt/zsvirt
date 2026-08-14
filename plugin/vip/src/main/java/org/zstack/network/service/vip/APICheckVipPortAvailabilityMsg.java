package org.zstack.network.service.vip;
import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.message.APIGetMessage;

@RestRequest(
        path = "/vips/{vipUuid}/check-port-availability",
        method = HttpMethod.GET,
        responseClass = APICheckVipPortAvailabilityReply.class
)
public class APICheckVipPortAvailabilityMsg extends APIGetMessage {

    @APIParam(resourceType = VipVO.class)
    private String vipUuid;    
    
    @APIParam(numberRange = {1, 65535})
    private int port;    

    @APIParam(validValues = {"TCP", "UDP"})
    private String protocolType;

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    } 

    public String getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(String protocolType) {
        this.protocolType = protocolType;
    }


    public static APICheckVipPortAvailabilityMsg __example__() {
        APICheckVipPortAvailabilityMsg msg = new APICheckVipPortAvailabilityMsg();
        msg.setVipUuid(uuid());
        msg.setPort(5);
        msg.setProtocolType("TCP");
        return msg;
    }
}
