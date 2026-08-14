package org.zstack.softwarePackage.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.softwarePackage.entity.SoftwarePackageVO;

import java.util.concurrent.TimeUnit;

@RestRequest(
        path = "/software-packages/backup-storage/{uuid}/upgrade",
        method = HttpMethod.POST,
        responseClass = APIUploadAndExecuteSoftwareUpgradePackageEvent.class,
        parameterName = "params"
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 12)
public class APIUploadAndExecuteSoftwareUpgradePackageMsg extends APIMessage {
    @APIParam(resourceType = SoftwarePackageVO.class)
    private String uuid;

    @APIParam(required = false, resourceType = BackupStorageVO.class)
    private String backupStorageUuid;

    @NoLogging(type = NoLogging.Type.Uri)
    @APIParam(required = false, maxLength = 1024)
    private String url;

    @APIParam(required = false, maxLength = 1024)
    private String installPath;

    @APIParam(required = false, validValues = {"Normal", "Reexecute"})
    private String upgradeType = "Normal";

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public String getUpgradeType() {
        return upgradeType;
    }

    public void setUpgradeType(String upgradeType) {
        this.upgradeType = upgradeType;
    }

    public static APIUploadAndExecuteSoftwareUpgradePackageMsg __example__() {
        APIUploadAndExecuteSoftwareUpgradePackageMsg msg = new APIUploadAndExecuteSoftwareUpgradePackageMsg();
        msg.setUuid(uuid(SoftwarePackageVO.class));
        return msg;
    }
}
