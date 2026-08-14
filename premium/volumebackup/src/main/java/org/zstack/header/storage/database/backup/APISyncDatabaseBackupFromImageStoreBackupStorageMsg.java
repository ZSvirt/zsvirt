package org.zstack.header.storage.database.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;

@RestRequest(
        path = "/database-backups/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APISyncDatabaseBackupFromImageStoreBackupStorageEvent.class
)
public class APISyncDatabaseBackupFromImageStoreBackupStorageMsg extends APIMessage implements SyncDatabaseBackupMessage {
    @APIParam(resourceType = DatabaseBackupVO.class)
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

    @Override
    public String getSrcBackupStorageUuid() {
        return srcBackupStorageUuid;
    }

    public void setSrcBackupStorageUuid(String srcBackupStorageUuid) {
        this.srcBackupStorageUuid = srcBackupStorageUuid;
    }

    @Override
    public String getDstBackupStorageUuid() {
        return dstBackupStorageUuid;
    }

    public void setDstBackupStorageUuid(String dstBackupStorageUuid) {
        this.dstBackupStorageUuid = dstBackupStorageUuid;
    }

    public static APISyncDatabaseBackupFromImageStoreBackupStorageMsg __example__() {
        APISyncDatabaseBackupFromImageStoreBackupStorageMsg msg = new APISyncDatabaseBackupFromImageStoreBackupStorageMsg();

        msg.setUuid(uuid());
        msg.setSrcBackupStorageUuid(uuid());
        msg.setDstBackupStorageUuid(uuid());

        return msg;
    }

    @Override
    public String getDatabaseBackupUuid() {
        return uuid;
    }
}
