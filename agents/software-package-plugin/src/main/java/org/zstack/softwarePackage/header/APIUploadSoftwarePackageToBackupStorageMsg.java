package org.zstack.softwarePackage.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.*;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.softwarePackage.entity.SoftwarePackageVO;

import java.util.concurrent.TimeUnit;

@RestRequest(
        path = "/software-packages/backup-storage/upload",
        method = HttpMethod.POST,
        responseClass = APIUploadSoftwarePackageToBackupStorageEvent.class,
        parameterName = "params"
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 12)
public class APIUploadSoftwarePackageToBackupStorageMsg extends APICreateMessage implements APIAuditor {
    @APIParam(maxLength = 255)
    private String name;

    @APIParam(maxLength = 255)
    private String type;

    @APIParam(required = false, resourceType = BackupStorageVO.class)
    private String backupStorageUuid;

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

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
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

    public static APIUploadSoftwarePackageToBackupStorageMsg __example__() {
        APIUploadSoftwarePackageToBackupStorageMsg msg = new APIUploadSoftwarePackageToBackupStorageMsg();
        msg.setName("software-package-name");
        msg.setType("ZMigrate");
        msg.setBackupStorageUuid(uuid(BackupStorageVO.class));
        msg.setUrl("http://192.168.1.1/disk/images/test.qcow2");
        msg.setInstallPath("/root/zmigrate.tar.gz");
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        if (!rsp.isSuccess()) {
            return new Result("", SoftwarePackageVO.class);
        }
        SoftwarePackageInventory inventory = ((APIUploadSoftwarePackageToBackupStorageEvent) rsp).getInventory();
        return new Result(inventory != null ? inventory.getUuid() : "", SoftwarePackageVO.class);
    }
}
