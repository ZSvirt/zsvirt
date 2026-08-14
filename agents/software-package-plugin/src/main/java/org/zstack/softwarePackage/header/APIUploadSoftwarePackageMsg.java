package org.zstack.softwarePackage.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
import org.zstack.header.log.NoLogging;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.message.*;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.softwarePackage.entity.SoftwarePackageVO;

import java.util.concurrent.TimeUnit;

@RestRequest(
        path = "/software-packages/upload",
        method = HttpMethod.POST,
        responseClass = APIUploadSoftwarePackageEvent.class,
        parameterName = "params"
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 72)
public class APIUploadSoftwarePackageMsg extends APICreateMessage implements APIAuditor {
    @APIParam(maxLength = 255)
    private String name;

    @APIParam(maxLength = 255)
    private String type;

    @APIParam(resourceType = ManagementNodeVO.class)
    private String managementNodeUuid;

    @APIParam(resourceType = HostVO.class)
    private String hostUuid;

    @NoLogging(type = NoLogging.Type.Uri)
    @APIParam(maxLength = 1024)
    private String url;

    @APIParam(maxLength = 1024)
    private String installPath;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getManagementNodeUuid() {
        return managementNodeUuid;
    }

    public void setManagementNodeUuid(String managementNodeUuid) {
        this.managementNodeUuid = managementNodeUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public static APIUploadSoftwarePackageMsg __example__() {
        APIUploadSoftwarePackageMsg msg = new APIUploadSoftwarePackageMsg();
        msg.setName("software-package-name");
        msg.setType("storage");
        msg.setManagementNodeUuid(uuid(ManagementNodeVO.class));
        msg.setHostUuid(uuid(HostVO.class));
        msg.setUrl("http://192.168.1.1/disk/images/test.qcow2");
        msg.setInstallPath("/root/sds/storage.tar.gz");
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        if (!rsp.isSuccess()) {
            return new Result("", SoftwarePackageVO.class);
        }
        SoftwarePackageInventory inventory = ((APIUploadSoftwarePackageEvent) rsp).getInventory();
        return new Result(inventory != null ? inventory.getUuid() : "", SoftwarePackageVO.class);
    }
}
