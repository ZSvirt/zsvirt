package org.zstack.softwarePackage.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.softwarePackage.entity.SoftwarePackageVO;

@RestRequest(
        path = "/software-package/upload-jobs/details/{softwarePackageId}",
        method = HttpMethod.GET,
        responseClass = APIGetUploadSoftwarePackageJobDetailsReply.class
)
public class APIGetUploadSoftwarePackageJobDetailsMsg extends APISyncCallMessage {
    @APIParam
    private String softwarePackageId;

    public String getSoftwarePackageId() {
        return softwarePackageId;
    }

    public void setSoftwarePackageId(String softwarePackageId) {
        this.softwarePackageId = softwarePackageId;
    }

    public static APIGetUploadSoftwarePackageJobDetailsMsg __example__() {
        APIGetUploadSoftwarePackageJobDetailsMsg msg = new APIGetUploadSoftwarePackageJobDetailsMsg();
        msg.softwarePackageId = uuid(SoftwarePackageVO.class);
        return msg;
    }
}