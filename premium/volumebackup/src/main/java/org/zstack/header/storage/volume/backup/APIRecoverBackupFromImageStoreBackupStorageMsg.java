package org.zstack.header.storage.volume.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageMessage;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.VolumeBackupVO;

@RestRequest(
        path = "/volume-backups/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIRecoverBackupFromImageStoreBackupStorageEvent.class
)
public class APIRecoverBackupFromImageStoreBackupStorageMsg extends APIMessage implements BackupStorageMessage {
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

    public static APIRecoverBackupFromImageStoreBackupStorageMsg __example__() {
        APIRecoverBackupFromImageStoreBackupStorageMsg msg = new APIRecoverBackupFromImageStoreBackupStorageMsg();

        msg.setUuid(uuid());
        msg.setSrcBackupStorageUuid(uuid());
        msg.setDstBackupStorageUuid(uuid());

        return msg;
    }

    @Override
    public String getBackupStorageUuid() {
        return srcBackupStorageUuid;
    }
}
