package org.zstack.header.storage.snapshot.group;

import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Created by MaJin on 2019/7/9.
 */
@Entity
@Table
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = VolumeSnapshotGroupVO.class, myField = "volumeSnapshotGroupUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = VolumeSnapshotVO.class, myField = "volumeSnapshotUuid", targetField = "uuid"),
        }
)
public class VolumeSnapshotGroupRefVO implements Serializable {
    @Id
    @Column
    private String volumeSnapshotUuid;

    @Column
    @ForeignKey(parentEntityClass = VolumeSnapshotGroupVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String volumeSnapshotGroupUuid;

    @Column
    private boolean snapshotDeleted = false;

    @Column
    private Integer deviceId;

    @Column
    private String volumeUuid;

    @Column
    private String volumeName;

    @Column
    private String volumeType;

    @Column
    private String volumeSnapshotInstallPath;

    @Column
    private String volumeSnapshotName;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @Column
    private Timestamp volumeLastAttachDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

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

    public Integer getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
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

    public String getVolumeSnapshotName() {
        return volumeSnapshotName;
    }

    public void setVolumeSnapshotName(String volumeSnapshotName) {
        this.volumeSnapshotName = volumeSnapshotName;
    }

    public String getVolumeSnapshotInstallPath() {
        return volumeSnapshotInstallPath;
    }

    public void setVolumeSnapshotInstallPath(String volumeSnapshotInstallPath) {
        this.volumeSnapshotInstallPath = volumeSnapshotInstallPath;
    }

    public boolean isSnapshotDeleted() {
        return snapshotDeleted;
    }

    public void setSnapshotDeleted(boolean snapshotDeleted) {
        this.snapshotDeleted = snapshotDeleted;
    }

    public Timestamp getVolumeLastAttachDate() {
        return volumeLastAttachDate;
    }

    public void setVolumeLastAttachDate(Timestamp volumeLastAttachDate) {
        this.volumeLastAttachDate = volumeLastAttachDate;
    }
}
