package org.zstack.header.storage.snapshot;

import org.zstack.header.message.APIEvent;

/**
 * @apiResult api event for message :ref:`APIBackupVolumeSnapshotMsg`
 * @category volume snapshot
 * @example {
 * "org.zstack.header.storage.snapshot.APIBackupVolumeSnapshotEvent": {
 * "inventory": {
 * "uuid": "6c8c6b0ea9844ff3bc58cc46b2fde6ce",
 * "name": "Snapshot-565e50b3c6ab4eb19c3d0dc66b36b3f9",
 * "description": "Test snapshot",
 * "type": "Hypervisor",
 * "volumeUuid": "565e50b3c6ab4eb19c3d0dc66b36b3f9",
 * "treeUuid": "2e1bea0124eb4b08b88bee3a5fd3d51a",
 * "hypervisorType": "KVM",
 * "parentUuid": "b95dd4de16f8486d8de38c014891b7cd",
 * "primaryStorageUuid": "8e0fbd85f5064c19aad766ae8adb9081",
 * "primaryStorageInstallPath": "/opt/zstack/nfsprimarystorage/prim-8e0fbd85f5064c19aad766ae8adb9081/dataVolumes/acct-36c27e8ff05c4780bf6d2fa65700f22e/vol-565e50b3c6ab4eb19c3d0dc66b36b3f9/snapshots/6c8c6b0ea9844ff3bc58cc46b2fde6ce.qcow2",
 * "type": "Data",
 * "latest": true,
 * "size": 10485760,
 * "state": "Enabled",
 * "status": "Ready",
 * "createDate": "May 3, 2014 12:00:53 PM",
 * "lastOpDate": "May 3, 2014 12:00:53 PM",
 * "backupStorageRefs": [
 * {
 * "volumeSnapshotUuid": "6c8c6b0ea9844ff3bc58cc46b2fde6ce",
 * "backupStorageUuid": "9656aa7cc6fb46ebab65aedc12a4728c",
 * "installPath": "nfs:/test1/volumeSnapshots/acct-36c27e8ff05c4780bf6d2fa65700f22e/6c8c6b0ea9844ff3bc58cc46b2fde6ce/6c8c6b0ea9844ff3bc58cc46b2fde6ce.qcow2"
 * }
 * ]
 * },
 * "success": true
 * }
 * }
 * @since 0.1.0
 */
public class APIBackupVolumeSnapshotEvent extends APIEvent {
    /**
     * @desc see :ref:`VolumeSnapshotInventory`
     */
    private VolumeSnapshotInventory inventory;

    public APIBackupVolumeSnapshotEvent(String apiId) {
        super(apiId);
    }

    public APIBackupVolumeSnapshotEvent() {
        super(null);
    }

    public VolumeSnapshotInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeSnapshotInventory inventory) {
        this.inventory = inventory;
    }
 

    public static APIBackupVolumeSnapshotEvent __example__() {
        APIBackupVolumeSnapshotEvent msg = new APIBackupVolumeSnapshotEvent();
        return msg;
    }
    
}