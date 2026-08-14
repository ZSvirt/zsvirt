package org.zstack.header.storage.snapshot.reference;

import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
public class VolumeSnapshotReferenceTreeVO extends ResourceVO implements ToInventory {
    @Column
    private String primaryStorageUuid;
    @Column
    private String hostUuid;
    @Column
    private String rootImageUuid;
    @Column
    private String rootVolumeSnapshotTreeUuid;
    @Column
    private String rootVolumeSnapshotUuid;
    @Column
    private String rootVolumeUuid;
    @Column
    private String rootInstallUrl;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public String getRootImageUuid() {
        return rootImageUuid;
    }

    public void setRootImageUuid(String rootImageUuid) {
        this.rootImageUuid = rootImageUuid;
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
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

    public String getRootVolumeSnapshotTreeUuid() {
        return rootVolumeSnapshotTreeUuid;
    }

    public void setRootVolumeSnapshotTreeUuid(String rootVolumeSnapshotTreeUuid) {
        this.rootVolumeSnapshotTreeUuid = rootVolumeSnapshotTreeUuid;
    }

    public String getRootVolumeSnapshotUuid() {
        return rootVolumeSnapshotUuid;
    }

    public void setRootVolumeSnapshotUuid(String rootVolumeSnapshotUuid) {
        this.rootVolumeSnapshotUuid = rootVolumeSnapshotUuid;
    }

    public String getRootInstallUrl() {
        return rootInstallUrl;
    }

    public void setRootInstallUrl(String rootVolumeSnapshotInstallUrl) {
        this.rootInstallUrl = rootVolumeSnapshotInstallUrl;
    }

    public String getRootVolumeUuid() {
        return rootVolumeUuid;
    }

    public void setRootVolumeUuid(String rootVolumeUuid) {
        this.rootVolumeUuid = rootVolumeUuid;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
