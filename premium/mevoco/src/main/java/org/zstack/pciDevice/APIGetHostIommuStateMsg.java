package org.zstack.pciDevice;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/pci-device/hosts/{uuid}/state",
        method = HttpMethod.GET,
        responseClass = APIGetHostIommuStateReply.class
)
public class APIGetHostIommuStateMsg extends APISyncCallMessage {
    @APIParam(resourceType = HostVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIGetHostIommuStateMsg __example__() {
        APIGetHostIommuStateMsg msg = new APIGetHostIommuStateMsg();
        msg.setUuid(uuid());

        return msg;
    }
}
