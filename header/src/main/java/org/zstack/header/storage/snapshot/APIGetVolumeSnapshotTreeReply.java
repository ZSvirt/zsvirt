package org.zstack.header.storage.snapshot;

import org.zstack.header.message.APIReply;

import java.util.List;

/**
 * @category volume snapshot
 * @example {
 * "org.zstack.header.storage.snapshot.APIGetVolumeSnapshotTreeReply": {
 * "inventories": [
 * {
 * "uuid": "4c4fdfe0ec4b47528c23047b140ed577",
 * "volumeUuid": "d71b1fffebb143549dadbecd82aac998",
 * "current": true,
 * "tree": {
 * "inventory": {
 * "uuid": "59187fd8ae914927b8b3be7c51aae035",
 * "name": "Snapshot-d71b1fffebb143549dadbecd82aac998",
 * "description": "Test snapshot",
 * "type": "Hypervisor",
 * "volumeUuid": "d71b1fffebb143549dadbecd82aac998",
 * "treeUuid": "4c4fdfe0ec4b47528c23047b140ed577",
 * "hypervisorType": "KVM",
 * "primaryStorageUuid": "342ecf7e70a44f6ba81dc0533aad2b8d",
 * "primaryStorageInstallPath": "/opt/zstack/nfsprimarystorage/prim-342ecf7e70a44f6ba81dc0533aad2b8d/rootVolumes/acct-36c27e8ff05c4780bf6d2fa65700f22e/vol-d71b1fffebb143549dadbecd82aac998/snapshots/59187fd8ae914927b8b3be7c51aae035.qcow2",
 * "type": "Root",
 * "latest": false,
 * "size": 10485760,
 * "state": "Enabled",
 * "status": "Ready",
 * "createDate": "May 3, 2014 12:17:22 PM",
 * "lastOpDate": "May 3, 2014 12:17:22 PM",
 * "backupStorageRefs": [
 * {
 * "volumeSnapshotUuid": "59187fd8ae914927b8b3be7c51aae035",
 * "backupStorageUuid": "23a96d7b4305453f9413020efaca64b2",
 * "installPath": "nfs:/test/volumeSnapshots/acct-36c27e8ff05c4780bf6d2fa65700f22e/59187fd8ae914927b8b3be7c51aae035/59187fd8ae914927b8b3be7c51aae035.qcow2"
 * }
 * ]
 * },
 * "children": [
 * {
 * "inventory": {
 * "uuid": "7ba07e804fd24a8fa6b2a3f04bb8ad94",
 * "name": "Snapshot-d71b1fffebb143549dadbecd82aac998",
 * "description": "Test snapshot",
 * "type": "Hypervisor",
 * "volumeUuid": "d71b1fffebb143549dadbecd82aac998",
 * "treeUuid": "4c4fdfe0ec4b47528c23047b140ed577",
 * "hypervisorType": "KVM",
 * "parentUuid": "59187fd8ae914927b8b3be7c51aae035",
 * "primaryStorageUuid": "342ecf7e70a44f6ba81dc0533aad2b8d",
 * "primaryStorageInstallPath": "/opt/zstack/nfsprimarystorage/prim-342ecf7e70a44f6ba81dc0533aad2b8d/rootVolumes/acct-36c27e8ff05c4780bf6d2fa65700f22e/vol-d71b1fffebb143549dadbecd82aac998/snapshots/7ba07e804fd24a8fa6b2a3f04bb8ad94.qcow2",
 * "type": "Root",
 * "latest": false,
 * "size": 10485760,
 * "state": "Enabled",
 * "status": "Ready",
 * "createDate": "May 3, 2014 12:17:22 PM",
 * "lastOpDate": "May 3, 2014 12:17:22 PM",
 * "backupStorageRefs": [
 * {
 * "volumeSnapshotUuid": "7ba07e804fd24a8fa6b2a3f04bb8ad94",
 * "backupStorageUuid": "23a96d7b4305453f9413020efaca64b2",
 * "installPath": "nfs:/test/volumeSnapshots/acct-36c27e8ff05c4780bf6d2fa65700f22e/7ba07e804fd24a8fa6b2a3f04bb8ad94/7ba07e804fd24a8fa6b2a3f04bb8ad94.qcow2"
 * }
 * ]
 * },
 * "parentUuid": "59187fd8ae914927b8b3be7c51aae035",
 * "children": [
 * {
 * "inventory": {
 * "uuid": "e90f94533871408ab945396653208026",
 * "name": "Snapshot-d71b1fffebb143549dadbecd82aac998",
 * "description": "Test snapshot",
 * "type": "Hypervisor",
 * "volumeUuid": "d71b1fffebb143549dadbecd82aac998",
 * "treeUuid": "4c4fdfe0ec4b47528c23047b140ed577",
 * "hypervisorType": "KVM",
 * "parentUuid": "7ba07e804fd24a8fa6b2a3f04bb8ad94",
 * "primaryStorageUuid": "342ecf7e70a44f6ba81dc0533aad2b8d",
 * "primaryStorageInstallPath": "/opt/zstack/nfsprimarystorage/prim-342ecf7e70a44f6ba81dc0533aad2b8d/rootVolumes/acct-36c27e8ff05c4780bf6d2fa65700f22e/vol-d71b1fffebb143549dadbecd82aac998/snapshots/e90f94533871408ab945396653208026.qcow2",
 * "type": "Root",
 * "latest": false,
 * "size": 10485760,
 * "state": "Enabled",
 * "status": "Ready",
 * "createDate": "May 3, 2014 12:17:22 PM",
 * "lastOpDate": "May 3, 2014 12:17:22 PM",
 * "backupStorageRefs": [
 * {
 * "volumeSnapshotUuid": "e90f94533871408ab945396653208026",
 * "backupStorageUuid": "23a96d7b4305453f9413020efaca64b2",
 * "installPath": "nfs:/test/volumeSnapshots/acct-36c27e8ff05c4780bf6d2fa65700f22e/e90f94533871408ab945396653208026/e90f94533871408ab945396653208026.qcow2"
 * }
 * ]
 * },
 * "parentUuid": "7ba07e804fd24a8fa6b2a3f04bb8ad94",
 * "children": [
 * {
 * "inventory": {
 * "uuid": "bf534fd8305d4c56aa3842b2c3dd52ab",
 * "name": "Snapshot-d71b1fffebb143549dadbecd82aac998",
 * "description": "Test snapshot",
 * "type": "Hypervisor",
 * "volumeUuid": "d71b1fffebb143549dadbecd82aac998",
 * "treeUuid": "4c4fdfe0ec4b47528c23047b140ed577",
 * "hypervisorType": "KVM",
 * "parentUuid": "e90f94533871408ab945396653208026",
 * "primaryStorageUuid": "342ecf7e70a44f6ba81dc0533aad2b8d",
 * "primaryStorageInstallPath": "/opt/zstack/nfsprimarystorage/prim-342ecf7e70a44f6ba81dc0533aad2b8d/rootVolumes/acct-36c27e8ff05c4780bf6d2fa65700f22e/vol-d71b1fffebb143549dadbecd82aac998/snapshots/bf534fd8305d4c56aa3842b2c3dd52ab.qcow2",
 * "type": "Root",
 * "latest": true,
 * "size": 10485760,
 * "state": "Enabled",
 * "status": "Ready",
 * "createDate": "May 3, 2014 12:17:22 PM",
 * "lastOpDate": "May 3, 2014 12:17:22 PM",
 * "backupStorageRefs": [
 * {
 * "volumeSnapshotUuid": "bf534fd8305d4c56aa3842b2c3dd52ab",
 * "backupStorageUuid": "23a96d7b4305453f9413020efaca64b2",
 * "installPath": "nfs:/test/volumeSnapshots/acct-36c27e8ff05c4780bf6d2fa65700f22e/bf534fd8305d4c56aa3842b2c3dd52ab/bf534fd8305d4c56aa3842b2c3dd52ab.qcow2"
 * }
 * ]
 * },
 * "parentUuid": "e90f94533871408ab945396653208026",
 * "children": []
 * }
 * ]
 * }
 * ]
 * }
 * ]
 * },
 * "createDate": "May 3, 2014 12:17:22 PM",
 * "lastOpDate": "May 3, 2014 12:17:22 PM"
 * }
 * ],
 * "success": true
 * }
 * }
 * @since 0.1.0
 */

public class APIGetVolumeSnapshotTreeReply extends APIReply {
    /**
     * @desc a list of volume snapshot tree. See :ref:`VolumeSnapshotTreeInventory`
     */
    private List<VolumeSnapshotTreeInventory> inventories;

    public List<VolumeSnapshotTreeInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VolumeSnapshotTreeInventory> inventories) {
        this.inventories = inventories;
    }


    public static APIGetVolumeSnapshotTreeReply __example__() {
        APIGetVolumeSnapshotTreeReply msg = new APIGetVolumeSnapshotTreeReply();
        return msg;
    }
    
}