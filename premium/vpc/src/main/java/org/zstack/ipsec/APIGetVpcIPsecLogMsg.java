package org.zstack.ipsec;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;

/**
 * created by boce.wang 07/09/2022
 */
@RestRequest(
        path = "/vpc/virtual-routers/ipseclog",
        method = HttpMethod.GET,
        responseClass = APIGetVpcIPsecLogReply.class
)
public class APIGetVpcIPsecLogMsg extends APISyncCallMessage {
    @APIParam(resourceType = VirtualRouterVmVO.class)
    private String uuid;

    @APIParam(required = false)
    private Integer lines = 30;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Integer getLines() {
        return lines;
    }

    public void setLines(Integer lines) {
        this.lines = lines;
    }

    public static APIGetVpcIPsecLogMsg  __example__() {
        APIGetVpcIPsecLogMsg msg = new APIGetVpcIPsecLogMsg();
        msg.setUuid(uuid());
        msg.setLines(30);
        return msg;
    }
}
