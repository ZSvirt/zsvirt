package org.zstack.vpc;


import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.service.VirtualRouterHaAPIMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vpc.VpcMessage;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;

/**
 * created by boce.wang 24/06/2022
 */
@RestRequest(
        path = "/vpc/virtual-routers/{uuid}/softwareversion",
        method = HttpMethod.POST,
        responseClass = APIUpdateVirtualRouterSoftwareVersionEvent.class,
        parameterName = "params"
)
public class APIUpdateVirtualRouterSoftwareVersionMsg extends APIMessage implements VpcMessage, VirtualRouterHaAPIMessage {
    @APIParam(resourceType = VirtualRouterVmVO.class)
    private String uuid;

    @APIParam(validValues = {"IPsec"})
    private String softwareName;

    @APIParam(validValues = {"4.5.2", "5.9.4", "5.8.4"})
    private String targetVersion;

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

    public String getTargetVersion() {
        return targetVersion;
    }

    public void setTargetVersion(String targetVersion) {
        this.targetVersion = targetVersion;
    }

    @Override
    public String getVirtualRouterUuid() {
        return uuid;
    }

    @Override
    public void setVirtualRouterUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getVpcRouterUuid() {
        return uuid;
    }

    public static APIUpdateVirtualRouterSoftwareVersionMsg __example__() {
        APIUpdateVirtualRouterSoftwareVersionMsg msg = new APIUpdateVirtualRouterSoftwareVersionMsg();
        msg.setUuid(uuid());
        msg.setSoftwareName("IPsec");
        msg.setTargetVersion("5.9.4");
        return msg;
    }
}
