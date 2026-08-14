package org.zstack.header.storage.database.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.backup.SyncBackupResult;

@RestResponse(allTo = "result")
public class APISyncDatabaseBackupEvent extends APIEvent {
    private SyncBackupResult result;

    public APISyncDatabaseBackupEvent(String apiId) {
        super(apiId);
    }

    public APISyncDatabaseBackupEvent() {
        super();
    }

    public static APISyncDatabaseBackupEvent __example__() {
        SyncBackupResult result = new SyncBackupResult(1, 3);
        APISyncDatabaseBackupEvent event = new APISyncDatabaseBackupEvent(uuid(DatabaseBackupVO.class));
        event.result = result;
        return event;
    }

    public SyncBackupResult getResult() {
        return result;
    }

    public void setResult(SyncBackupResult result) {
        this.result = result;
    }
}
