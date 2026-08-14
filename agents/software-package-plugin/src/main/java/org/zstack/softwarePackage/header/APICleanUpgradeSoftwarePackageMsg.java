package org.zstack.softwarePackage.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.softwarePackage.entity.SoftwarePackageVO;

@RestRequest(
        path = "/software-package/upgrade/packages/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APICleanUpgradeSoftwarePackageEvent.class
)
public class APICleanUpgradeSoftwarePackageMsg extends APIDeleteMessage {
    @APIParam(resourceType = SoftwarePackageVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APICleanUpgradeSoftwarePackageMsg __example__() {
        APICleanUpgradeSoftwarePackageMsg msg = new APICleanUpgradeSoftwarePackageMsg();
        msg.setUuid(uuid(SoftwarePackageVO.class));
        return msg;
    }
}
