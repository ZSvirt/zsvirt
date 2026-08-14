package org.zstack.storage.ceph.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.backup.BackupStorageState;
import org.zstack.header.storage.backup.BackupStorageStatus;

import java.sql.Timestamp;
import java.util.Collections;

/**
 * Created by frank on 8/1/2015.
 */
@RestResponse(allTo = "inventory")
public class APIAddMonToCephBackupStorageEvent extends APIEvent {
    private CephBackupStorageInventory inventory;

    public APIAddMonToCephBackupStorageEvent() {
    }

    public APIAddMonToCephBackupStorageEvent(String apiId) {
        super(apiId);
    }

    public CephBackupStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(CephBackupStorageInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIAddMonToCephBackupStorageEvent __example__() {
        APIAddMonToCephBackupStorageEvent event = new APIAddMonToCephBackupStorageEvent();

        CephBackupStorageInventory bs = new CephBackupStorageInventory();
        bs.setName("My Ceph Backup Storage");
        bs.setDescription("Public Ceph Backup Storage");
        bs.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        bs.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        bs.setType("Ceph");
        CephBackupStorageMonInventory mon = new CephBackupStorageMonInventory();
        mon.setMonUuid(uuid());
        mon.setMonAddr("10.0.1.2");
        bs.setMons(Collections.singletonList(mon));
        bs.setState(BackupStorageState.Enabled.toString());
        bs.setStatus(BackupStorageStatus.Connected.toString());
        bs.setAvailableCapacity(924L * 1024L * 1024L);
        bs.setTotalCapacity(1024L * 1024L * 1024L);
        bs.setAttachedZoneUuids(Collections.singletonList(uuid()));

        event.setInventory(bs);
        return event;
    }

}
