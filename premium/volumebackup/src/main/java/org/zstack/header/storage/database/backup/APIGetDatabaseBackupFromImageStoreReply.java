package org.zstack.header.storage.database.backup;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@RestResponse(fieldsTo = {"all"})
public class APIGetDatabaseBackupFromImageStoreReply extends APIReply {
    private List<DatabaseBackupStruct> backups = new ArrayList<>();

    public List<DatabaseBackupStruct> getBackups() {
        return backups;
    }

    public void setBackups(List<DatabaseBackupStruct> backups) {
        this.backups = backups;
    }

    public static APIGetDatabaseBackupFromImageStoreReply __example__() {
        APIGetDatabaseBackupFromImageStoreReply reply = new APIGetDatabaseBackupFromImageStoreReply();
        DatabaseBackupStruct struct = new DatabaseBackupStruct();
        struct.setName("zstack-db-backup");
        struct.setCreatedTime(new Timestamp(org.zstack.header.message.DocUtils.date));
        struct.setVersion("3.0.0");
        struct.setInstallPath("zstore://zsbak/0ed599ec519249489475112a058bb93a");
        reply.backups.add(struct);
        return reply;
    }
}
