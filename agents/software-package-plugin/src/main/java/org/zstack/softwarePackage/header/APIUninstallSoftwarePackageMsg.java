package org.zstack.softwarePackage.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.softwarePackage.entity.SoftwarePackageVO;

@RestRequest(
        path = "/software-package/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUninstallSoftwarePackageEvent.class
)
public class APIUninstallSoftwarePackageMsg extends APIMessage {
    @APIParam(resourceType = SoftwarePackageVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @APINoSee
    private SoftwarePackageVO softwarePackageVO;

    public SoftwarePackageVO getSoftwarePackageVO() {
        return softwarePackageVO;
    }

    public void setSoftwarePackageVO(SoftwarePackageVO softwarePackageVO) {
        this.softwarePackageVO = softwarePackageVO;
    }

    public static APIUninstallSoftwarePackageMsg __example__() {
        APIUninstallSoftwarePackageMsg msg = new APIUninstallSoftwarePackageMsg();
        msg.setUuid(uuid(SoftwarePackageVO.class));
        return msg;
    }
}
