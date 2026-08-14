package org.zstack.header.storage.volume.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageMessage;
import org.zstack.header.storage.backup.BackupStorageVO;

import java.util.concurrent.TimeUnit;

@RestRequest(
        path = "/vm-backups/{groupUuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APISyncVmBackupFromImageStoreBackupStorageEvent.class
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 24)
public class APISyncVmBackupFromImageStoreBackupStorageMsg extends APIMessage implements BackupStorageMessage {
    @APIParam
    private String groupUuid;

    @APIParam(resourceType = BackupStorageVO.class)
    private String srcBackupStorageUuid;

    @APIParam(resourceType = BackupStorageVO.class)
    private String dstBackupStorageUuid;

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }

    public String getSrcBackupStorageUuid() {
        return srcBackupStorageUuid;
    }

    public void setSrcBackupStorageUuid(String srcBackupStorageUuid) {
        this.srcBackupStorageUuid = srcBackupStorageUuid;
    }

    public String getDstBackupStorageUuid() {
        return dstBackupStorageUuid;
    }

    public void setDstBackupStorageUuid(String dstBackupStorageUuid) {
        this.dstBackupStorageUuid = dstBackupStorageUuid;
    }

    public static APISyncVmBackupFromImageStoreBackupStorageMsg __example__() {
        APISyncVmBackupFromImageStoreBackupStorageMsg msg = new APISyncVmBackupFromImageStoreBackupStorageMsg();

        msg.setGroupUuid(uuid());
        msg.setSrcBackupStorageUuid(uuid());
        msg.setDstBackupStorageUuid(uuid());

        return msg;
    }

    @Override
    public String getBackupStorageUuid() {
        return dstBackupStorageUuid;
    }
}
