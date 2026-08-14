package org.zstack.header.storage.snapshot.group;

import org.zstack.header.query.*;
import org.zstack.header.search.Inventory;

import javax.persistence.JoinColumn;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by MaJin on 2019/7/9.
 */
@Inventory(mappingVOClass = VolumeSnapshotGroupVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "volumeSnapshotRef", inventoryClass = VolumeSnapshotGroupRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "volumeSnapshotGroupUuid", hidden = true),
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "volumeSnapshot", expandedField = "volumeSnapshotRef.volumeSnapshot")
})
public class VolumeSnapshotGroupInventory {
    private String uuid;
    private Integer snapshotCount;
    private String name;
    private String description;
    private String vmInstanceUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    @Queryable(mappingClass = VolumeSnapshotGroupRefInventory.class,
            joinColumn = @JoinColumn(name = "volumeSnapshotGroupUuid"))
    private List<VolumeSnapshotGroupRefInventory> volumeSnapshotRefs;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Integer getSnapshotCount() {
        return snapshotCount;
    }

    public void setSnapshotCount(Integer snapshotCount) {
        this.snapshotCount = snapshotCount;
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

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
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

    public static VolumeSnapshotGroupInventory valueOf(VolumeSnapshotGroupVO vo) {
        VolumeSnapshotGroupInventory inv = new VolumeSnapshotGroupInventory();
        inv.setUuid(vo.getUuid());
        inv.setSnapshotCount(vo.getSnapshotCount());
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        inv.setVmInstanceUuid(vo.getVmInstanceUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setVolumeSnapshotRefs(VolumeSnapshotGroupRefInventory.valueOf(vo.getVolumeSnapshotRefs()));
        return inv;
    }

    public static List<VolumeSnapshotGroupInventory> valueOf(Collection<VolumeSnapshotGroupVO> vos) {
        return vos.stream().map(VolumeSnapshotGroupInventory::valueOf).collect(Collectors.toList());
    }

    public List<VolumeSnapshotGroupRefInventory> getVolumeSnapshotRefs() {
        return volumeSnapshotRefs;
    }

    public void setVolumeSnapshotRefs(List<VolumeSnapshotGroupRefInventory> volumeSnapshotRefs) {
        this.volumeSnapshotRefs = volumeSnapshotRefs;
    }
}
