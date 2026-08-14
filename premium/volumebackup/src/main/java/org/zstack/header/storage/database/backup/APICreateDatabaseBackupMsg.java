package org.zstack.header.storage.database.backup;

import org.springframework.http.HttpMethod;
import org.zstack.core.Platform;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;

import java.util.concurrent.TimeUnit;

@RestRequest(
        path = "/database-backups",
        method = HttpMethod.POST,
        responseClass = APICreateDatabaseBackupEvent.class,
        parameterName = "params"
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 12)
public class APICreateDatabaseBackupMsg extends APICreateMessage {
    @APIParam(maxLength = 255, emptyString = false)
    private String name;

    @APIParam(maxLength = 1024, emptyString = false, required = false)
    private String description;

    @APIParam(resourceType = BackupStorageVO.class)
    private String backupStorageUuid;

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public static APICreateDatabaseBackupMsg __example__() {
        APICreateDatabaseBackupMsg msg = new APICreateDatabaseBackupMsg();
        msg.setBackupStorageUuid(uuid(BackupStorageVO.class));
        msg.setName("zstack-db");
        return msg;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
