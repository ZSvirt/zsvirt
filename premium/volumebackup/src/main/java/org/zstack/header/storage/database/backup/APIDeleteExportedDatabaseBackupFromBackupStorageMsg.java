package org.zstack.header.storage.database.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;

@RestRequest(
        path = "/exported-database-backup/{databaseBackupUuid}/backup-storage/{backupStorageUuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteExportedDatabaseBackupFromBackupStorageEvent.class
)
public class APIDeleteExportedDatabaseBackupFromBackupStorageMsg extends APIMessage implements DatabaseBackupMessage {
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
    
    public static APIDeleteExportedDatabaseBackupFromBackupStorageMsg __example__() {
        APIDeleteExportedDatabaseBackupFromBackupStorageMsg msg = new APIDeleteExportedDatabaseBackupFromBackupStorageMsg();

        msg.setBackupStorageUuid(uuid());
        msg.setDatabaseBackupUuid(uuid());

        return msg;
    }
}
