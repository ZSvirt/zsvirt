package org.zstack.header.storage.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.Collections;

/**
 * @apiResult api event for message :ref:`APIDetachBackupStorageMsg`
 * @example {
 * "org.zstack.header.storage.backup.APIDetachBackupStorageEvent": {
 * "inventory": {
 * "uuid": "18421b64c18c458a8f362203c73593e1",
 * "name": "SimulatoryBackupStorage-0",
 * "url": "nfs://simulator/backupstorage/-0",
 * "totalCapacity": 107374182400,
 * "availableCapacity": 107374182400,
 * "type": "SimulatorBackupStorage",
 * "state": "Enabled",
 * "status": "Connected",
 * "createDate": "May 2, 2014 12:23:20 AM",
 * "lastOpDate": "May 2, 2014 12:23:20 AM",
 * "attachedZoneUuids": []
 * },
 * "success": true
 * }
 * }
 * @since 0.1.0
 */
@RestResponse(allTo = "inventory")
public class APIDetachBackupStorageFromZoneEvent extends APIEvent {
    public APIDetachBackupStorageFromZoneEvent() {
        super(null);
    }

    public APIDetachBackupStorageFromZoneEvent(String apiId) {
        super(apiId);
    }

    /**
     * @desc see :ref:`BackupStorageInventory`
     */
    private BackupStorageInventory inventory;

    public BackupStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(BackupStorageInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIDetachBackupStorageFromZoneEvent __example__() {
        APIDetachBackupStorageFromZoneEvent event = new APIDetachBackupStorageFromZoneEvent();

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

        event.setInventory(bs);
        return event;
    }

}
