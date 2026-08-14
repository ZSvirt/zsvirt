package org.zstack.header.storage.volume.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageMessage;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.VolumeBackupVO;

import java.util.concurrent.TimeUnit;

@RestRequest(
        path = "/volume-backups/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APISyncBackupFromImageStoreBackupStorageEvent.class
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 24)
public class APISyncBackupFromImageStoreBackupStorageMsg extends APIMessage implements BackupStorageMessage {
    @APIParam(resourceType = VolumeBackupVO.class)
    private String uuid;
    @APIParam(resourceType = BackupStorageVO.class)
    private String srcBackupStorageUuid;
    @APIParam(resourceType = BackupStorageVO.class)
    private String dstBackupStorageUuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public static APISyncBackupFromImageStoreBackupStorageMsg __example__() {
        APISyncBackupFromImageStoreBackupStorageMsg msg = new APISyncBackupFromImageStoreBackupStorageMsg();

        msg.setUuid(uuid());
        msg.setSrcBackupStorageUuid(uuid());
        msg.setDstBackupStorageUuid(uuid());

        return msg;
    }

    @Override
    public String getBackupStorageUuid() {
        return dstBackupStorageUuid;
    }
}
