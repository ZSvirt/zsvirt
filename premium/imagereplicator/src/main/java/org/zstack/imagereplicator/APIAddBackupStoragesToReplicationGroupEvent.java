package org.zstack.imagereplicator;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIAddBackupStoragesToReplicationGroupEvent extends APIEvent {
    private List<ImageReplicationGroupBackupStorageRefInventory> inventories;

    public List<ImageReplicationGroupBackupStorageRefInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ImageReplicationGroupBackupStorageRefInventory> inventories) {
        this.inventories = inventories;
    }

    public APIAddBackupStoragesToReplicationGroupEvent() {
        super(null);
    }

    public APIAddBackupStoragesToReplicationGroupEvent(String apiId) {
        super(apiId);
    }

    public static APIAddBackupStoragesToReplicationGroupEvent __example__() {
        APIAddBackupStoragesToReplicationGroupEvent evt = new APIAddBackupStoragesToReplicationGroupEvent();
        Timestamp time = new Timestamp(org.zstack.header.message.DocUtils.date);
        String replicationGroupUuid = uuid();

        ImageReplicationGroupBackupStorageRefInventory inv1 = new ImageReplicationGroupBackupStorageRefInventory();
        inv1.setReplicationGroupUuid(replicationGroupUuid);
        inv1.setBackupStorageUuid(uuid());
        inv1.setCreateDate(time);
        inv1.setLastOpDate(time);

        ImageReplicationGroupBackupStorageRefInventory inv2 = new ImageReplicationGroupBackupStorageRefInventory();
        inv2.setReplicationGroupUuid(replicationGroupUuid);
        inv2.setBackupStorageUuid(uuid());
        inv2.setCreateDate(time);
        inv1.setLastOpDate(time);

        List<ImageReplicationGroupBackupStorageRefInventory> inventories = new ArrayList<>();
        inventories.add(inv1);
        inventories.add(inv2);
        evt.setInventories(inventories);
        return evt;
    }
}
