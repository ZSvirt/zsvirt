package org.zstack.header.storage.database.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteDatabaseBackupEvent extends APIEvent {
    public APIDeleteDatabaseBackupEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteDatabaseBackupEvent() {
        super(null);
    }

    public static APIDeleteDatabaseBackupEvent __example__(){
        return new APIDeleteDatabaseBackupEvent();
    }
}
