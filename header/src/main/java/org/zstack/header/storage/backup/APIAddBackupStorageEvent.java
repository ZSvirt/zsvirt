package org.zstack.header.storage.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.Collections;

/**
 * @apiResult api event for message :ref:`APIAddBackupStorageMsg`
 * @example {
 * "org.zstack.header.storage.backup.APIAddBackupStorageEvent": {
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
 * "attachedZoneUuids": []
 * },
 * "success": true
 * }
 * }
 * @since 0.1.0
 */

@RestResponse(allTo = "inventory")
public class APIAddBackupStorageEvent extends APIEvent {
    /**
     * @desc see :ref:`BackupStorageInventory`
     */
    private BackupStorageInventory inventory;

    public APIAddBackupStorageEvent(String apiId) {
        super(apiId);
    }

    public APIAddBackupStorageEvent() {
        super(null);
    }

    public BackupStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(BackupStorageInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIAddBackupStorageEvent __example__() {
        APIAddBackupStorageEvent event = new APIAddBackupStorageEvent();

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
