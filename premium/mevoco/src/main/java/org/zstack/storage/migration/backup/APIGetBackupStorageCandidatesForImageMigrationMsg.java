package org.zstack.storage.migration.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.storage.migration.StorageMigrationMessage;

/**
 * Created by GuoYi on 9/21/17.
 */
@RestRequest(
        path = "/backup-storage/{srcBackupStorageUuid}/migration-candidates",
        method = HttpMethod.GET,
        responseClass = APIGetBackupStorageCandidatesForImageMigrationReply.class
)
public class APIGetBackupStorageCandidatesForImageMigrationMsg extends APISyncCallMessage implements StorageMigrationMessage {
    @APIParam(resourceType = BackupStorageVO.class)
    private String srcBackupStorageUuid;

    public static APIGetBackupStorageCandidatesForImageMigrationMsg __example__() {
        APIGetBackupStorageCandidatesForImageMigrationMsg msg = new APIGetBackupStorageCandidatesForImageMigrationMsg();
        msg.setSrcBackupStorageUuid(uuid());
        return msg;
    }

    public String getSrcBackupStorageUuid() {
        return srcBackupStorageUuid;
    }

    public void setSrcBackupStorageUuid(String srcBackupStorageUuid) {
        this.srcBackupStorageUuid = srcBackupStorageUuid;
    }
}
