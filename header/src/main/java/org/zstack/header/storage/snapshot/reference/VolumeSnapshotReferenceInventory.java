package org.zstack.header.storage.snapshot.reference;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.volume.VolumeInventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = VolumeSnapshotReferenceVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "volume", inventoryClass = VolumeInventory.class,
                foreignKey = "volumeUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "referenceVolume", inventoryClass = VolumeInventory.class,
                foreignKey = "referenceVolumeUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "volumeSnapshot", inventoryClass = VolumeSnapshotInventory.class,
                foreignKey = "volumeSnapshotUuid", expandedInventoryKey = "uuid"),
})
public class VolumeSnapshotReferenceInventory {
    private long id;

    private Long parentId;
    private String volumeUuid;

    private String volumeSnapshotUuid;

    private String volumeSnapshotInstallUrl;

    private String directSnapshotUuid;

    private String directSnapshotInstallUrl;

    /**
     * the UUID of resource referencing @volumeUuid.
     */
    private String referenceUuid;

    private String referenceType;

    private String referenceInstallUrl;

    private String referenceVolumeUuid;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    public static VolumeSnapshotReferenceInventory valueOf(VolumeSnapshotReferenceVO vo) {
        VolumeSnapshotReferenceInventory inv = new VolumeSnapshotReferenceInventory();
        inv.id = vo.getId();
        inv.parentId = vo.getParentId();
        inv.volumeUuid = vo.getVolumeUuid();
        inv.volumeSnapshotUuid = vo.getVolumeSnapshotUuid();
        inv.directSnapshotUuid = vo.getDirectSnapshotUuid();
        inv.volumeSnapshotInstallUrl = vo.getVolumeSnapshotInstallUrl();
        inv.directSnapshotInstallUrl = vo.getDirectSnapshotInstallUrl();
        inv.referenceUuid = vo.getReferenceUuid();
        inv.referenceType = vo.getReferenceType();
        inv.referenceInstallUrl = vo.getReferenceInstallUrl();
        inv.referenceVolumeUuid = vo.getReferenceVolumeUuid();
        inv.createDate = vo.getCreateDate();
        inv.lastOpDate = vo.getLastOpDate();
        return inv;
    }

    public static List<VolumeSnapshotReferenceInventory> valueOf(Collection<VolumeSnapshotReferenceVO> vos) {
        return vos.stream().map(VolumeSnapshotReferenceInventory::valueOf).collect(Collectors.toList());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getVolumeSnapshotUuid() {
        return volumeSnapshotUuid;
    }

    public void setVolumeSnapshotUuid(String volumeSnapshotUuid) {
        this.volumeSnapshotUuid = volumeSnapshotUuid;
    }

    public String getVolumeSnapshotInstallUrl() {
        return volumeSnapshotInstallUrl;
    }

    public void setVolumeSnapshotInstallUrl(String volumeSnapshotInstallUrl) {
        this.volumeSnapshotInstallUrl = volumeSnapshotInstallUrl;
    }

    public String getReferenceUuid() {
        return referenceUuid;
    }

    public void setReferenceUuid(String referenceUuid) {
        this.referenceUuid = referenceUuid;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getReferenceInstallUrl() {
        return referenceInstallUrl;
    }

    public void setReferenceInstallUrl(String referenceInstallUrl) {
        this.referenceInstallUrl = referenceInstallUrl;
    }

    public String getReferenceVolumeUuid() {
        return referenceVolumeUuid;
    }

    public void setReferenceVolumeUuid(String referenceVolumeUuid) {
        this.referenceVolumeUuid = referenceVolumeUuid;
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

    public String getDirectSnapshotUuid() {
        return directSnapshotUuid;
    }

    public void setDirectSnapshotUuid(String directSnapshotUuid) {
        this.directSnapshotUuid = directSnapshotUuid;
    }

    public String getDirectSnapshotInstallUrl() {
        return directSnapshotInstallUrl;
    }

    public void setDirectSnapshotInstallUrl(String directSnapshotInstallUrl) {
        this.directSnapshotInstallUrl = directSnapshotInstallUrl;
    }
}
