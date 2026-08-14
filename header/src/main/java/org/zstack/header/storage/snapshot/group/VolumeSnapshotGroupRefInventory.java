package org.zstack.header.storage.snapshot.group;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by MaJin on 2019/7/9.
 */
@Inventory(mappingVOClass = VolumeSnapshotGroupRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "volumeSnapshotGroup", inventoryClass = VolumeSnapshotGroupRefInventory.class,
                foreignKey = "volumeSnapshotGroupUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "volumeSnapshot", inventoryClass = VolumeSnapshotInventory.class,
                foreignKey = "volumeSnapshotUuid", expandedInventoryKey = "uuid")
})
public class VolumeSnapshotGroupRefInventory {
    private String volumeSnapshotUuid;
    private String volumeSnapshotGroupUuid;
    private int deviceId;
    private boolean snapshotDeleted;
    private String volumeUuid;
    private String volumeName;
    private String volumeType;
    private String volumeSnapshotInstallPath;
    private String volumeSnapshotName;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private Timestamp volumeLastAttachDate;

    public String getVolumeSnapshotUuid() {
        return volumeSnapshotUuid;
    }

    public void setVolumeSnapshotUuid(String volumeSnapshotUuid) {
        this.volumeSnapshotUuid = volumeSnapshotUuid;
    }

    public String getVolumeSnapshotGroupUuid() {
        return volumeSnapshotGroupUuid;
    }

    public void setVolumeSnapshotGroupUuid(String volumeSnapshotGroupUuid) {
        this.volumeSnapshotGroupUuid = volumeSnapshotGroupUuid;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public boolean isSnapshotDeleted() {
        return snapshotDeleted;
    }

    public void setSnapshotDeleted(boolean snapshotDeleted) {
        this.snapshotDeleted = snapshotDeleted;
    }

    public static VolumeSnapshotGroupRefInventory valueOf(VolumeSnapshotGroupRefVO vo) {
        VolumeSnapshotGroupRefInventory ref = new VolumeSnapshotGroupRefInventory();
        ref.deviceId = vo.getDeviceId();
        ref.volumeSnapshotGroupUuid = vo.getVolumeSnapshotGroupUuid();
        ref.volumeSnapshotUuid = vo.getVolumeSnapshotUuid();
        ref.snapshotDeleted = vo.isSnapshotDeleted();
        ref.volumeName = vo.getVolumeName();
        ref.volumeUuid = vo.getVolumeUuid();
        ref.volumeType = vo.getVolumeType();
        ref.volumeSnapshotInstallPath = vo.getVolumeSnapshotInstallPath();
        ref.volumeSnapshotName = vo.getVolumeSnapshotName();
        ref.createDate = vo.getCreateDate();
        ref.lastOpDate = vo.getLastOpDate();
        ref.volumeLastAttachDate = vo.getVolumeLastAttachDate();
        return ref;
    }

    public static List<VolumeSnapshotGroupRefInventory> valueOf(Collection<VolumeSnapshotGroupRefVO> vos) {
        return vos.stream().map(VolumeSnapshotGroupRefInventory::valueOf).collect(Collectors.toList());
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

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getVolumeName() {
        return volumeName;
    }

    public void setVolumeName(String volumeName) {
        this.volumeName = volumeName;
    }

    public String getVolumeType() {
        return volumeType;
    }

    public void setVolumeType(String volumeType) {
        this.volumeType = volumeType;
    }

    public String getVolumeSnapshotInstallPath() {
        return volumeSnapshotInstallPath;
    }

    public void setVolumeSnapshotInstallPath(String volumeSnapshotInstallPath) {
        this.volumeSnapshotInstallPath = volumeSnapshotInstallPath;
    }

    public String getVolumeSnapshotName() {
        return volumeSnapshotName;
    }

    public void setVolumeSnapshotName(String volumeSnapshotName) {
        this.volumeSnapshotName = volumeSnapshotName;
    }

    public Timestamp getVolumeLastAttachDate() {
        return volumeLastAttachDate;
    }

    public void setVolumeLastAttachDate(Timestamp volumeLastAttachDate) {
        this.volumeLastAttachDate = volumeLastAttachDate;
    }
}
