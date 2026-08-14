package org.zstack.header.storage.database.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;

@RestRequest(
        path = "/database-backups/{databaseBackupUuid}/backup-storage/{backupStorageUuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIExportDatabaseBackupFromBackupStorageEvent.class
)
public class APIExportDatabaseBackupFromBackupStorageMsg extends APIMessage implements DatabaseBackupMessage {
    @APIParam(resourceType = BackupStorageVO.class)
    private String backupStorageUuid;

    @APIParam(resourceType = DatabaseBackupVO.class)
    private String databaseBackupUuid;

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    @Override
    public String getDatabaseBackupUuid() {
        return databaseBackupUuid;
    }

    public void setDatabaseBackupUuid(String databaseBackupUuid) {
        this.databaseBackupUuid = databaseBackupUuid;
    }

    public static APIExportDatabaseBackupFromBackupStorageMsg __example__() {
        APIExportDatabaseBackupFromBackupStorageMsg msg = new APIExportDatabaseBackupFromBackupStorageMsg();

        msg.setBackupStorageUuid(uuid());
        msg.setDatabaseBackupUuid(uuid());

        return msg;
    }
}
