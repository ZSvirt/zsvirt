package org.zstack.header.storage.backup;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryBackupStorageReply extends APIQueryReply {
    private List<BackupStorageInventory> inventories;

    public List<BackupStorageInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<BackupStorageInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryBackupStorageReply __example__() {
        APIQueryBackupStorageReply reply = new APIQueryBackupStorageReply();

        BackupStorageInventory bs = new BackupStorageInventory();
        bs.setName("My Backup Storage");
        bs.setDescription("Public Backup Storage");
        bs.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        bs.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        bs.setType("Ceph");
        bs.setState(BackupStorageState.Enabled.toString());
        bs.setStatus(BackupStorageStatus.Connected.toString());
        bs.setAvailableCapacity(924L * 1024L * 1024L);
        bs.setTotalCapacity(1024L * 1024L * 1024L);
        bs.setAttachedZoneUuids(Collections.singletonList(uuid()));

        reply.setInventories(Collections.singletonList(bs));

        return reply;
    }

}
