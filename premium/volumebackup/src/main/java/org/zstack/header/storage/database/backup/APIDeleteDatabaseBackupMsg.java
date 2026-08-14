package org.zstack.header.storage.database.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;

import java.util.Collections;
import java.util.List;

@RestRequest(
        path = "/database-backups/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteDatabaseBackupEvent.class
)
public class APIDeleteDatabaseBackupMsg extends APIMessage implements DeleteDatabaseBackupMessage {
    @APIParam(resourceType = DatabaseBackupVO.class, successIfResourceNotExisting = true)
    private String uuid;

    @APIParam(resourceType = BackupStorageVO.class, required = false)
    private List<String> backupStorageUuids;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getDatabaseBackupUuid() {
        return uuid;
    }

    @Override
    public List<String> getBackupStorageUuids() {
        return backupStorageUuids;
    }

    @Override
    public boolean isDbOnly() {
        return false;
    }

    public void setBackupStorageUuids(List<String> backupStorageUuids) {
        this.backupStorageUuids = backupStorageUuids;
    }

    public static APIDeleteDatabaseBackupMsg __example__() {
        APIDeleteDatabaseBackupMsg msg = new APIDeleteDatabaseBackupMsg();
        msg.setBackupStorageUuids(Collections.singletonList(uuid()));
        msg.setUuid(uuid());
        return msg;
    }
}
