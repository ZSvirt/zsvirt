package org.zstack.storage.backup.sftp;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIReconnectSftpBackupStorageEvent extends APIEvent {
    private SftpBackupStorageInventory inventory;
    
    public APIReconnectSftpBackupStorageEvent(String apiId) {
        super(apiId);
    }
    
    public APIReconnectSftpBackupStorageEvent() {
        super(null);
    }

    public SftpBackupStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(SftpBackupStorageInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIReconnectSftpBackupStorageEvent __example__() {
        APIReconnectSftpBackupStorageEvent event = new APIReconnectSftpBackupStorageEvent();
        SftpBackupStorageInventory inventory = new SftpBackupStorageInventory();
        inventory.setUsername("tester");
        inventory.setHostname("192.168.0.1");
        inventory.setSshPort(8000);

        return event;
    }

}
