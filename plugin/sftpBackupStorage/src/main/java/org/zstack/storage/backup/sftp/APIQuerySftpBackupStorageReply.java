package org.zstack.storage.backup.sftp;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQuerySftpBackupStorageReply extends APIQueryReply {
    private List<SftpBackupStorageInventory> inventories;

    public List<SftpBackupStorageInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SftpBackupStorageInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQuerySftpBackupStorageReply __example__() {
        APIQuerySftpBackupStorageReply reply = new APIQuerySftpBackupStorageReply();
        List<SftpBackupStorageInventory> inventories = new ArrayList<SftpBackupStorageInventory>();
        SftpBackupStorageInventory inventory = new SftpBackupStorageInventory();
        inventory.setSshPort(8000);
        inventory.setHostname("192.168.0.1");
        inventory.setUsername("tester");
        inventories.add(inventory);
        reply.setInventories(inventories);

        return reply;
    }

}
