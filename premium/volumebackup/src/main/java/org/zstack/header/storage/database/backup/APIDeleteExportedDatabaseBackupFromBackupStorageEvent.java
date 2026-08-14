package org.zstack.header.storage.database.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteExportedDatabaseBackupFromBackupStorageEvent extends APIEvent {
    public APIDeleteExportedDatabaseBackupFromBackupStorageEvent(){
        super();
    }

    public APIDeleteExportedDatabaseBackupFromBackupStorageEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteExportedDatabaseBackupFromBackupStorageEvent __example__(){
        return new APIDeleteExportedDatabaseBackupFromBackupStorageEvent();
    }
}
