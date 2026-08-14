package org.zstack.header.storage.backup;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = VolumeBackupStorageRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "volumeBackup", inventoryClass = VolumeBackupInventory.class,
                foreignKey = "volumeBackupUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "backupStorage", inventoryClass = BackupStorageInventory.class,
                foreignKey = "backupStorageUuid", expandedInventoryKey = "uuid"),
})
public class VolumeBackupStorageRefInventory implements Serializable {
    @APINoSee
    private long id;
    private String volumeBackupUuid;
    private String backupStorageUuid;
    private String installPath;
    private String status;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static VolumeBackupStorageRefInventory valueOf(VolumeBackupStorageRefVO vo) {
        VolumeBackupStorageRefInventory inv = new VolumeBackupStorageRefInventory();
        inv.setId(vo.getId());
        inv.setVolumeBackupUuid(vo.getVolumeBackupUuid());
        inv.setBackupStorageUuid(vo.getBackupStorageUuid());
        inv.setStatus(vo.getStatus().toString());
        inv.setInstallPath(vo.getInstallPath());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<VolumeBackupStorageRefInventory> valueOf(Collection<VolumeBackupStorageRefVO> vos) {
        return vos.stream().map(VolumeBackupStorageRefInventory::valueOf)
                .collect(Collectors.toList());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getVolumeBackupUuid() {
        return volumeBackupUuid;
    }

    public void setVolumeBackupUuid(String volumeBackupUuid) {
        this.volumeBackupUuid = volumeBackupUuid;
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
