package org.zstack.header.storage.database.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"all"})
public class APIExportDatabaseBackupFromBackupStorageEvent extends APIEvent {
    private String databaseBackupUrl;

    public String getDatabaseBackupUrl() {
        return databaseBackupUrl;
    }

    public void setDatabaseBackupUrl(String databaseBackupUrl) {
        this.databaseBackupUrl = databaseBackupUrl;
    }

    public APIExportDatabaseBackupFromBackupStorageEvent(){
        super();
    }

    public APIExportDatabaseBackupFromBackupStorageEvent(String apiId) {
        super(apiId);
    }

    public static APIExportDatabaseBackupFromBackupStorageEvent __example__() {
        APIExportDatabaseBackupFromBackupStorageEvent event = new APIExportDatabaseBackupFromBackupStorageEvent();
        event.setDatabaseBackupUrl("http://127.0.0.1:8001/path/zstack-db-backup.gz");
        return event;
    }
}

