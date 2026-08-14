package org.zstack.header.storage.snapshot;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.Unqueryable;
import org.zstack.header.search.Inventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotTree.SnapshotLeafInventory;
import org.zstack.header.volume.VolumeInventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @inventory inventory for volume snapshot tree
 * @category volume snapshot
 * @example {
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
 * ]
 * }
 * @since 0.1.0
 */
@Inventory(mappingVOClass = VolumeSnapshotTreeVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "volume", inventoryClass = VolumeInventory.class,
                foreignKey = "volumeUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "snapshot", inventoryClass = VolumeSnapshotInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "treeUuid"),
})
public class VolumeSnapshotTreeInventory {
    /**
     * @desc volume snapshot tree uuid
     */
    private String uuid;
    /**
     * @desc uuid of volume the tree was created from
     */
    private String volumeUuid;
    /**
     * @desc true if the next snapshot will be created on this tree, false if not
     * @choices - true
     * - false
     */
    private Boolean current;

    /**
     * @desc describe the status of the SnapshotTree
     */
    private String status;
    /**
     * @desc tree inventory, see :ref:`SnapshotLeafInventory`
     */
    @Unqueryable
    private SnapshotLeafInventory tree;
    /**
     * @desc the time this resource gets created
     */
    private Timestamp createDate;
    /**
     * @desc last time this resource gets operated
     */
    private Timestamp lastOpDate;

    public static VolumeSnapshotTreeInventory valueOf(VolumeSnapshotTreeVO vo) {
        VolumeSnapshotTreeInventory inv = new VolumeSnapshotTreeInventory();
        inv.setCreateDate(vo.getCreateDate());
        inv.setCurrent(vo.isCurrent());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setUuid(vo.getUuid());
        inv.setVolumeUuid(vo.getVolumeUuid());
        inv.setStatus(vo.getStatus().toString());
        return inv;
    }

    public static List<VolumeSnapshotTreeInventory> valueOf(Collection<VolumeSnapshotTreeVO> vos) {
        List<VolumeSnapshotTreeInventory> invs = new ArrayList<VolumeSnapshotTreeInventory>();
        for (VolumeSnapshotTreeVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public SnapshotLeafInventory getTree() {
        return tree;
    }

    public void setTree(SnapshotLeafInventory tree) {
        this.tree = tree;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
