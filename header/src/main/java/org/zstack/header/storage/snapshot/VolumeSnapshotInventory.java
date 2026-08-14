package org.zstack.header.storage.snapshot;

import org.zstack.header.query.*;
import org.zstack.header.search.Inventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefInventory;
import org.zstack.header.volume.VolumeInventory;

import javax.persistence.JoinColumn;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @inventory inventory for volume snapshot
 * @category volume snapshot
 * @example {
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
 * }
 * }
 * @since 0.1.0
 */
@Inventory(mappingVOClass = VolumeSnapshotVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "volume", inventoryClass = VolumeInventory.class,
                foreignKey = "volumeUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "tree", inventoryClass = VolumeSnapshotTreeInventory.class,
                foreignKey = "treeUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "primaryStorage", inventoryClass = PrimaryStorageInventory.class,
                foreignKey = "primaryStorageUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "backupStorageRef", inventoryClass = VolumeSnapshotBackupStorageRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "backupStorageUuid"),
        @ExpandedQuery(expandedField = "groupRef", inventoryClass = VolumeSnapshotGroupRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "volumeSnapshotUuid"),
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "backupStorage", expandedField = "backupStorageRef.backupStorage"),
        @ExpandedQueryAlias(alias = "group", expandedField = "groupRef.volumeSnapshotGroup")
})
public class VolumeSnapshotInventory {
    /**
     * @desc volume snapshot uuid
     */
    private String uuid;
    /**
     * @desc max length of 255 characters
     */
    private String name;
    /**
     * @desc max length of 2048 characters
     * @nullable
     */
    private String description;
    /**
     * @desc - Hypervisor: file based snapshot which is created by hypervisor. For example, QCOW2 snapshot in KVM
     * - Storage: storage based snapshot which is created by primary storage. For example, ISCSI vendor usually provides
     * their own means to create snapshot from block device
     * @choices - Hypervisor
     * - Storage
     */
    private String type;
    /**
     * @desc uuid of volume where the snapshot was created from
     */
    private String volumeUuid;
    /**
     * @desc uuid of volume snapshot tree the snapshot belongs to
     */
    private String treeUuid;

    /**
     * @desc parent snapshot uuid
     */
    private String parentUuid;
    /**
     * @desc primary storage uuid if the snapshot is on primary storage. Could be null
     * @nullable
     */
    private String primaryStorageUuid;
    /**
     * @desc path on primary storage if the snapshot is on primary storage. Could be null
     * @nullable
     */
    private String primaryStorageInstallPath;
    /**
     * @desc type of volume where the snapshot was created. See type of :ref:`VolumeInventory`
     * @choices - Root
     * - Data
     */
    private String volumeType;

    private String format;
    /**
     * @desc true if the snapshot is the last one of the snapshot branch, false if not
     * @choices - true
     * - false
     */
    private Boolean latest;
    /**
     * @desc snapshot size in bytes
     */
    private Long size;

    private int distance;
    /**
     * @desc - Enabled: ok for operations
     * - Disabled: volume cannot revert to this snapshot
     * @choices - Enabled
     * - Disabled
     */
    private String state;
    /**
     * @desc - Creating: the snapshot is being created from volume
     * - CreatingTemplate: a template is being created from the snapshot
     * - CreatingVolume: a volume is being created from the snapshot
     * - Ready: ok for operations
     * - BackingUp: the snapshot is being backed up to backup storage
     * - Deleting: the snapshot is being deleted
     * @choices - Creating
     * - CreatingTemplate
     * - CreatingVolume
     * - Ready
     * - BackingUp
     * - Deleting
     */
    private String status;
    /**
     * @desc the time this resource gets created
     */
    private Timestamp createDate;
    /**
     * @desc last time this resource gets operated
     */
    private Timestamp lastOpDate;
    /**
     * @desc a list of :ref:`VolumeSnapshotBackupStorageRefInventory` representing information of the snapshot on backup storage
     */
    @Queryable(mappingClass = VolumeSnapshotBackupStorageRefInventory.class,
            joinColumn = @JoinColumn(name = "volumeSnapshotUuid"))
    private List<VolumeSnapshotBackupStorageRefInventory> backupStorageRefs;

    @Queryable(mappingClass = VolumeSnapshotGroupRefInventory.class,
            joinColumn = @JoinColumn(name = "volumeSnapshotUuid", referencedColumnName = "volumeSnapshotGroupUuid"))
    private String groupUuid;

    public static VolumeSnapshotInventory valueOf(VolumeSnapshotVO vo) {
        VolumeSnapshotInventory inv = new VolumeSnapshotInventory();
        inv.setName(vo.getName());
        inv.setCreateDate(vo.getCreateDate());
        inv.setDescription(vo.getDescription());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setParentUuid(vo.getParentUuid());
        inv.setState(vo.getState().toString());
        inv.setType(vo.getType());
        inv.setVolumeUuid(vo.getVolumeUuid());
        inv.setFormat(vo.getFormat());
        inv.setUuid(vo.getUuid());
        inv.setStatus(vo.getStatus().toString());
        inv.setPrimaryStorageUuid(vo.getPrimaryStorageUuid());
        inv.setPrimaryStorageInstallPath(vo.getPrimaryStorageInstallPath());
        inv.setLatest(vo.isLatest());
        inv.setSize(vo.getSize());
        inv.setVolumeType(vo.getVolumeType());
        inv.setTreeUuid(vo.getTreeUuid());
        inv.setBackupStorageRefs(VolumeSnapshotBackupStorageRefInventory.valueOf(vo.getBackupStorageRefs()));
        if (vo.getGroupRef() != null) {
            inv.setGroupUuid(vo.getGroupRef().getVolumeSnapshotGroupUuid());
        }
        inv.setDistance(vo.getDistance());
        return inv;
    }

    public static VolumeSnapshotInventory valueOf(VolumeSnapshotGroupRefInventory ref) {
        VolumeSnapshotInventory inv = new VolumeSnapshotInventory();
        inv.setCreateDate(ref.getCreateDate());
        inv.setLastOpDate(ref.getLastOpDate());
        inv.setName(ref.getVolumeSnapshotName());
        inv.setVolumeUuid(ref.getVolumeUuid());
        inv.setUuid(ref.getVolumeSnapshotUuid());
        inv.setVolumeType(ref.getVolumeType());
        inv.setGroupUuid(ref.getVolumeSnapshotGroupUuid());
        return inv;

    }
    public static List<VolumeSnapshotInventory> valueOf(Collection<VolumeSnapshotVO> vos) {
        List<VolumeSnapshotInventory> invs = new ArrayList<VolumeSnapshotInventory>();
        for (VolumeSnapshotVO vo : vos) {
            invs.add(VolumeSnapshotInventory.valueOf(vo));
        }
        return invs;
    }

    public List<VolumeSnapshotBackupStorageRefInventory> getBackupStorageRefs() {
        return backupStorageRefs;
    }

    public boolean isOnBackupStorage(String backupStorageUuid) {
        for (VolumeSnapshotBackupStorageRefInventory ref : backupStorageRefs) {
            if (ref.getBackupStorageUuid().equals(backupStorageUuid)) {
                return true;
            }
        }

        return false;
    }

    public void setBackupStorageRefs(List<VolumeSnapshotBackupStorageRefInventory> backupStorageRefs) {
        this.backupStorageRefs = backupStorageRefs;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getTreeUuid() {
        return treeUuid;
    }

    public void setTreeUuid(String treeUuid) {
        this.treeUuid = treeUuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getParentUuid() {
        return parentUuid;
    }

    public void setParentUuid(String parentUuid) {
        this.parentUuid = parentUuid;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getPrimaryStorageInstallPath() {
        return primaryStorageInstallPath;
    }

    public void setPrimaryStorageInstallPath(String primaryStorageInstallPath) {
        this.primaryStorageInstallPath = primaryStorageInstallPath;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getVolumeType() {
        return volumeType;
    }

    public void setVolumeType(String volumeType) {
        this.volumeType = volumeType;
    }

    public boolean isLatest() {
        return latest;
    }

    public void setLatest(boolean latest) {
        this.latest = latest;
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }
}
