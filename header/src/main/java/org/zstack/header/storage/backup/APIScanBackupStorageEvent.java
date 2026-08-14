package org.zstack.header.storage.backup;

import org.zstack.header.message.APIEvent;

public class APIScanBackupStorageEvent extends APIEvent {
    public APIScanBackupStorageEvent(String apiId) {
        super(apiId);
    }

    public APIScanBackupStorageEvent() {
        super(null);
    }
 

    public static APIScanBackupStorageEvent __example__() {
        APIScanBackupStorageEvent msg = new APIScanBackupStorageEvent();
        return msg;
    }
    
}