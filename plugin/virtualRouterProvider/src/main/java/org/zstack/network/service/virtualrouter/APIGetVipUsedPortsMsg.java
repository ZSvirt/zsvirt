package org.zstack.network.service.virtualrouter;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.network.service.vip.VipVO;

/**
 */
@RestRequest(
        path = "/vips/{uuid}/usedports",
        method = HttpMethod.GET,
        responseClass = APIGetVipUsedPortsReply.class
)
public class APIGetVipUsedPortsMsg extends APISyncCallMessage {
    @APIParam(resourceType = VipVO.class)
    private String uuid;

    @APIParam(validValues = {"TCP", "UDP"})
    private String protocol;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public static APIGetVipUsedPortsMsg __example__() {
        APIGetVipUsedPortsMsg msg = new APIGetVipUsedPortsMsg();
        msg.setUuid(uuid());
        msg.setProtocol("TCP");
        return msg;
    }

}
