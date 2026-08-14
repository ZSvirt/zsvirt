package org.zstack.vpc;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;

/**
 * created by boce.wang 24/06/2022
 */
@RestRequest(
        path = "/vpc/virtual-routers/softwareversion",
        method = HttpMethod.GET,
        responseClass = APIGetVirtualRouterSoftwareVersionReply.class
)
public class APIGetVirtualRouterSoftwareVersionMsg extends APISyncCallMessage {
    @APIParam(resourceType = VirtualRouterVmVO.class, required = false)
    private String uuid;

    @APIParam(validValues = {"IPsec"})
    private String softwareName;

    @APIParam(required = false)
    private Boolean needUpdate = false;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getSoftwareName() {
        return softwareName;
    }

    public void setSoftwareName(String softwareName) {
        this.softwareName = softwareName;
    }

    public Boolean getNeedUpdate() {
        return needUpdate;
    }

    public void setNeedUpdate(Boolean needUpdate) {
        this.needUpdate = needUpdate;
    }

    public static APIGetVirtualRouterSoftwareVersionMsg __example__() {
        APIGetVirtualRouterSoftwareVersionMsg msg = new APIGetVirtualRouterSoftwareVersionMsg();
        msg.setUuid(uuid());
        msg.setSoftwareName("IPsec");

        return msg;
    }
}
