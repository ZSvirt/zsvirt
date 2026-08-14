package org.zstack.header.storage.snapshot;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.storage.backup.BackupStorageInventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @inventory information for volume snapshot on backup storage
 * @category volume snapshot
 * @example {
 * "volumeSnapshotUuid": "6c8c6b0ea9844ff3bc58cc46b2fde6ce",
 * "backupStorageUuid": "9656aa7cc6fb46ebab65aedc12a4728c",
 * "installPath": "nfs:/test1/volumeSnapshots/acct-36c27e8ff05c4780bf6d2fa65700f22e/6c8c6b0ea9844ff3bc58cc46b2fde6ce/6c8c6b0ea9844ff3bc58cc46b2fde6ce.qcow2"
 * }
 * @since 0.1.0
 */
@Inventory(mappingVOClass = VolumeSnapshotBackupStorageRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "volumeSnapshot", inventoryClass = VolumeSnapshotInventory.class,
                foreignKey = "volumeSnapshotUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "backupStorage", inventoryClass = BackupStorageInventory.class,
                foreignKey = "backupStorageUuid", expandedInventoryKey = "uuid"),
})
public class VolumeSnapshotBackupStorageRefInventory {
    /**
     * @desc volume snapshot uuid
     */
    private String volumeSnapshotUuid;
    /**
     * @desc backup storage uuid
     */
    private String backupStorageUuid;
    /**
     * @desc path the snapshot on backup storage. Depending on backup storage type, this field may have various meanings.
     * For example, for sftp backup storage, it's filesystem path
     */
    private String installPath;

    public static VolumeSnapshotBackupStorageRefInventory valueOf(VolumeSnapshotBackupStorageRefVO vo) {
        VolumeSnapshotBackupStorageRefInventory inv = new VolumeSnapshotBackupStorageRefInventory();
        inv.setBackupStorageUuid(vo.getBackupStorageUuid());
        inv.setInstallPath(vo.getInstallPath());
        inv.setVolumeSnapshotUuid(vo.getVolumeSnapshotUuid());
        return inv;
    }

    public static List<VolumeSnapshotBackupStorageRefInventory> valueOf(Collection<VolumeSnapshotBackupStorageRefVO> vos) {
        List<VolumeSnapshotBackupStorageRefInventory> invs = new ArrayList<VolumeSnapshotBackupStorageRefInventory>();
        for (VolumeSnapshotBackupStorageRefVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }

    public String getVolumeSnapshotUuid() {
        return volumeSnapshotUuid;
    }

    public void setVolumeSnapshotUuid(String volumeSnapshotUuid) {
        this.volumeSnapshotUuid = volumeSnapshotUuid;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }
}
