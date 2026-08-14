package org.zstack.header.storage.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.Collections;

/**
 * @apiResult api event for :ref:`APIAttachBackupStorageMsg`
 * @example {
 * "org.zstack.header.storage.backup.APIAttachBackupStorageEvent": {
 * "inventory": {
 * "hostname": "localhost",
 * "username": "root",
 * "uuid": "63d3c48ef0094079ab986a7c6bff671b",
 * "name": "sftp",
 * "url": "nfs://test",
 * "totalCapacity": 1099511627776,
 * "availableCapacity": 1099511627776,
 * "type": "SftpBackupStorage",
 * "state": "Enabled",
 * "status": "Connected",
 * "createDate": "May 1, 2014 8:41:50 PM",
 * "lastOpDate": "May 1, 2014 8:41:50 PM",
 * "attachedZoneUuids": [
 * "80c8f13195304f4bb009aea375d708fa"
 * ]
 * },
 * "success": true
 * }
 * }
 * @since 0.1.0
 */
@RestResponse(allTo = "inventory")
public class APIAttachBackupStorageToZoneEvent extends APIEvent {
    /**
     * @desc see :ref:`BackupStorageInventory`
     */
    private BackupStorageInventory inventory;

    public APIAttachBackupStorageToZoneEvent() {
        super(null);
    }

    public APIAttachBackupStorageToZoneEvent(String apiId) {
        super(apiId);
    }

    public BackupStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(BackupStorageInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIAttachBackupStorageToZoneEvent __example__() {
        APIAttachBackupStorageToZoneEvent event = new APIAttachBackupStorageToZoneEvent();

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
