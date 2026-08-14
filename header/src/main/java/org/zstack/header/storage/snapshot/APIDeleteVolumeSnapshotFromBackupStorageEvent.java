package org.zstack.header.storage.snapshot;

import org.zstack.header.message.APIEvent;

/**
 * @apiResult api event for message :ref:`APIDeleteVolumeSnapshotFromBackupStorageMsg`
 * @category volume snapshot
 * @example {
 * "org.zstack.header.storage.snapshot.APIDeleteVolumeSnapshotFromBackupStorageEvent": {
 * "inventory": {
 * "uuid": "789f13b8e9b84e44888b113e55c6e776",
 * "name": "Snapshot-8fc6d247a2b54984a707a65964f1d898",
 * "description": "Test snapshot",
 * "type": "Hypervisor",
 * "volumeUuid": "8fc6d247a2b54984a707a65964f1d898",
 * "treeUuid": "1f6b1e2901db4141a245b4a9542f1fbb",
 * "hypervisorType": "KVM",
 * "primaryStorageUuid": "1110a22387ba4a27807ceb8294bdc3f1",
 * "primaryStorageInstallPath": "/opt/zstack/nfsprimarystorage/prim-1110a22387ba4a27807ceb8294bdc3f1/rootVolumes/acct-36c27e8ff05c4780bf6d2fa65700f22e/vol-8fc6d247a2b54984a707a65964f1d898/snapshots/789f13b8e9b84e44888b113e55c6e776.qcow2",
 * "type": "Root",
 * "latest": false,
 * "size": 10485760,
 * "state": "Enabled",
 * "status": "Ready",
 * "createDate": "May 3, 2014 12:11:10 PM",
 * "lastOpDate": "May 3, 2014 12:11:10 PM",
 * "backupStorageRefs": [
 * {
 * "volumeSnapshotUuid": "789f13b8e9b84e44888b113e55c6e776",
 * "backupStorageUuid": "eb769c1ad24c473e9c6bdf94f862e0bc",
 * "installPath": "nfs:/test1/volumeSnapshots/acct-36c27e8ff05c4780bf6d2fa65700f22e/789f13b8e9b84e44888b113e55c6e776/789f13b8e9b84e44888b113e55c6e776.qcow2"
 * }
 * ]
 * },
 * "success": true
 * }
 * }
 * @since 0.1.0
 */
public class APIDeleteVolumeSnapshotFromBackupStorageEvent extends APIEvent {
    /**
     * @desc see :ref:`VolumeSnapshotInventory`
     */
    private VolumeSnapshotInventory inventory;

    public APIDeleteVolumeSnapshotFromBackupStorageEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteVolumeSnapshotFromBackupStorageEvent() {
        super(null);
    }

    public VolumeSnapshotInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeSnapshotInventory inventory) {
        this.inventory = inventory;
    }
 

    public static APIDeleteVolumeSnapshotFromBackupStorageEvent __example__() {
        APIDeleteVolumeSnapshotFromBackupStorageEvent msg = new APIDeleteVolumeSnapshotFromBackupStorageEvent();
        return msg;
    }
    
}