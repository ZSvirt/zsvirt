package org.zstack.zmigrate.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/zmigrate/management/infos",
        method = HttpMethod.GET,
        responseClass = APIGetZMigrateInfosReply.class
)
public class APIGetZMigrateInfosMsg extends APISyncCallMessage {

    @APINoSee
    private String softwarePackageUuid;

    @APINoSee
    private String managementVmUuid;

    public String getSoftwarePackageUuid() {
        return softwarePackageUuid;
    }

    public void setSoftwarePackageUuid(String softwarePackageUuid) {
        this.softwarePackageUuid = softwarePackageUuid;
    }

    public String getManagementVmUuid() {
        return managementVmUuid;
    }

    public void setManagementVmUuid(String managementVmUuid) {
        this.managementVmUuid = managementVmUuid;
    }

    public static APIGetZMigrateInfosMsg __example__() {
        APIGetZMigrateInfosMsg msg = new APIGetZMigrateInfosMsg();
        return msg;
    }
}
