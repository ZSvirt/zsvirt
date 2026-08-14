package org.zstack.pciDevice;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/pci-device/hosts/{uuid}/status",
        method = HttpMethod.GET,
        responseClass = APIGetHostIommuStatusReply.class
)
public class APIGetHostIommuStatusMsg extends APISyncCallMessage {
    @APIParam(resourceType = HostVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIGetHostIommuStatusMsg __example__() {
        APIGetHostIommuStatusMsg msg = new APIGetHostIommuStatusMsg();
        msg.setUuid(uuid());

        return msg;
    }
}
