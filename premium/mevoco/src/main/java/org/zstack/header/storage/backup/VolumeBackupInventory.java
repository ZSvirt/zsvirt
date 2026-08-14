package org.zstack.header.storage.backup;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.*;
import org.zstack.header.search.Inventory;

import javax.persistence.JoinColumn;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = VolumeBackupVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "backupStorageRef", inventoryClass = VolumeBackupStorageRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "volumeBackupUuid", hidden = true),
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "backupStorage", expandedField = "backupStorageRef.backupStorage")
})
public class VolumeBackupInventory {
    private String uuid;
    private String volumeUuid;
    private String name;
    private String description;
    private String type;
    private String state;
    private String status;
    private Long size;
    private String metadata;
    private String groupUuid;
    private BackupMode mode;
    private String vmInstanceUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    @Queryable(mappingClass = VolumeBackupStorageRefInventory.class,
            joinColumn = @JoinColumn(name = "volumeBackupUuid"))
    private List<VolumeBackupStorageRefInventory> backupStorageRefs;

    public static VolumeBackupInventory valueOf(VolumeBackupVO vo) {
        VolumeBackupInventory inv = new VolumeBackupInventory();
        inv.setName(vo.getName());
        inv.setCreateDate(vo.getCreateDate());
        inv.setDescription(vo.getDescription());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setVolumeUuid(vo.getVolumeUuid());
        inv.setUuid(vo.getUuid());
        inv.setSize(vo.getSize());
        inv.setType(vo.getType().toString());
        inv.setState(vo.getState().toString());
        inv.setStatus(vo.getStatus().toString());
        inv.setMetadata(vo.getMetadata());
        inv.setGroupUuid(vo.getGroupUuid());
        inv.setMode(vo.getMode());
        inv.setBackupStorageRefs(VolumeBackupStorageRefInventory.valueOf(vo.getBackupStorageRefs()));
        inv.setVmInstanceUuid(vo.getVmInstanceUuid());
        return inv;
    }

    public static List<VolumeBackupInventory> valueOf(Collection<VolumeBackupVO> vos) {
        return vos.stream()
                .map(VolumeBackupInventory::valueOf)
                .collect(Collectors.toList());
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

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }

    public List<VolumeBackupStorageRefInventory> getBackupStorageRefs() {
        return backupStorageRefs;
    }

    public void setBackupStorageRefs(List<VolumeBackupStorageRefInventory> backupStorageRefs) {
        this.backupStorageRefs = backupStorageRefs;
    }

    public BackupMode getMode() {
        return mode;
    }

    public void setMode(BackupMode mode) {
        this.mode = mode;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
