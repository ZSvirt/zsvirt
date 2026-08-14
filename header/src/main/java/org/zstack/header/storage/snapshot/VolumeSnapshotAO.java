package org.zstack.header.storage.snapshot;

import org.zstack.header.storage.primary.PrimaryStorageEO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.Index;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ShadowEntity;
import org.zstack.header.volume.VolumeEO;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 */
@MappedSuperclass
public class VolumeSnapshotAO extends ResourceVO implements ShadowEntity {
    @Column
    @Index
    private String name;

    @Column
    private String description;

    @Column
    private String type;

    @Column
    @ForeignKey(parentEntityClass = VolumeEO.class, onDeleteAction = ReferenceOption.SET_NULL)
    private String volumeUuid;

    @Column
    private String format;

    @Column
    @ForeignKey(parentEntityClass = VolumeSnapshotTreeEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String treeUuid;

    @Column
    @ForeignKey(parentEntityClass = VolumeSnapshotEO.class, onDeleteAction = ReferenceOption.SET_NULL)
    private String parentUuid;

    @Column
    @ForeignKey(parentEntityClass = PrimaryStorageEO.class, onDeleteAction = ReferenceOption.SET_NULL)
    private String primaryStorageUuid;

    @Column
    private String primaryStorageInstallPath;

    @Column
    private int distance;

    @Column
    private long size;

    @Column
    private boolean latest;

    @Column
    private boolean fullSnapshot;

    @Column
    private String volumeType;

    @Column
    @Enumerated(EnumType.STRING)
    private VolumeSnapshotState state;

    @Column
    @Enumerated(EnumType.STRING)
    private VolumeSnapshotStatus status;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @Transient
    private VolumeSnapshotAO shadow;

    public VolumeSnapshotAO getShadow() {
        return shadow;
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
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

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getParentUuid() {
        return parentUuid;
    }

    public void setParentUuid(String parentUuid) {
        this.parentUuid = parentUuid;
    }

    public VolumeSnapshotState getState() {
        return state;
    }

    public void setState(VolumeSnapshotState state) {
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

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public VolumeSnapshotStatus getStatus() {
        return status;
    }

    public void setStatus(VolumeSnapshotStatus status) {
        this.status = status;
    }

    public boolean isLatest() {
        return latest;
    }

    public void setLatest(boolean latest) {
        this.latest = latest;
    }

    public boolean isFullSnapshot() {
        return fullSnapshot;
    }

    public void setFullSnapshot(boolean fullSnapshot) {
        this.fullSnapshot = fullSnapshot;
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

    public void setVolumeType(String volumeType) {
        this.volumeType = volumeType;
    }

    public String getVolumeType() {
        return volumeType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public String getTreeUuid() {
        return treeUuid;
    }

    public void setTreeUuid(String treeUuid) {
        this.treeUuid = treeUuid;
    }

    @Override
    public void setShadow(Object o) {
        shadow = (VolumeSnapshotAO) o;
    }
}
