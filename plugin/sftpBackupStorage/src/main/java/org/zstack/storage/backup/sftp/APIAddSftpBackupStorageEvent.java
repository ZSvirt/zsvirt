package org.zstack.storage.backup.sftp;

import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.backup.APIAddBackupStorageEvent;

@RestResponse(allTo = "inventory")
public class APIAddSftpBackupStorageEvent extends APIAddBackupStorageEvent {
    public APIAddSftpBackupStorageEvent(String apiId) {
        super(apiId);
    }

    public APIAddSftpBackupStorageEvent() {
        super(null);
    }

    public static APIAddSftpBackupStorageEvent __example__() {
        APIAddSftpBackupStorageEvent event = new APIAddSftpBackupStorageEvent();
        SftpBackupStorageInventory ssInventory = new SftpBackupStorageInventory();
        ssInventory.setHostname("192.168.0.1");
        ssInventory.setSshPort(8080);
        ssInventory.setUsername("tester");
        event.setInventory(ssInventory);
        return event;
    }

}
