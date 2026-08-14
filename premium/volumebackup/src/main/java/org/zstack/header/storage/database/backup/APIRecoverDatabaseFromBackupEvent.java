package org.zstack.header.storage.database.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"all"})
public class APIRecoverDatabaseFromBackupEvent extends APIEvent {
    private int logListenPort;

    public APIRecoverDatabaseFromBackupEvent(){
        super();
    }

    public APIRecoverDatabaseFromBackupEvent(String apiId) {
        super(apiId);
    }

    public static APIRecoverDatabaseFromBackupEvent __example__(){
        APIRecoverDatabaseFromBackupEvent evt = new APIRecoverDatabaseFromBackupEvent();
        evt.setLogListenPort(10086);
        return new APIRecoverDatabaseFromBackupEvent();
    }

    public int getLogListenPort() {
        return logListenPort;
    }

    public void setLogListenPort(int logListenPort) {
        this.logListenPort = logListenPort;
    }
}
