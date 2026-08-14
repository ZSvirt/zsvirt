package org.zstack.softwarePackage.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.softwarePackage.entity.SoftwarePackageVO;

@RestRequest(
        path = "/software-package/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APICleanSoftwarePackageEvent.class
)
public class APICleanSoftwarePackageMsg extends APIDeleteMessage {
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

    public static APICleanSoftwarePackageMsg __example__() {
        APICleanSoftwarePackageMsg msg = new APICleanSoftwarePackageMsg();
        msg.setUuid(uuid(SoftwarePackageVO.class));
        return msg;
    }
}
