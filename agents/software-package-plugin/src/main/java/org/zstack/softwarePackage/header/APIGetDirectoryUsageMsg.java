package org.zstack.softwarePackage.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/software-package/directory/usage",
        method = HttpMethod.GET,
        responseClass = APIGetDirectoryUsageReply.class
)
public class APIGetDirectoryUsageMsg extends APISyncCallMessage {
    @APIParam(resourceType = ManagementNodeVO.class)
    private String managementNodeUuid;

    @APIParam(maxLength = 1024, emptyString = false)
    private String directoryPath;

    public String getManagementNodeUuid() {
        return managementNodeUuid;
    }

    public void setManagementNodeUuid(String managementNodeUuid) {
        this.managementNodeUuid = managementNodeUuid;
    }

    public String getDirectoryPath() {
        return directoryPath;
    }

    public void setDirectoryPath(String directoryPath) {
        this.directoryPath = directoryPath;
    }

    public static APIGetDirectoryUsageMsg __example__() {
        APIGetDirectoryUsageMsg msg = new APIGetDirectoryUsageMsg();
        msg.setManagementNodeUuid(uuid(ManagementNodeVO.class));
        msg.setDirectoryPath("/root/installPath/file");
        return msg;
    }
}
