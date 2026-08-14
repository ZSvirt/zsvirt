package org.zstack.header.storage.snapshot;

import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.volume.VolumeEO;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 */
@MappedSuperclass
public class VolumeSnapshotTreeAO extends ResourceVO {
    @Column
    @ForeignKey(parentEntityClass = VolumeEO.class, onDeleteAction = ReferenceOption.SET_NULL)
    private String volumeUuid;

    @Column
    private String rootImageUuid;

    @Column
    private boolean current;

    @Column
    @Enumerated(EnumType.STRING)
    private VolumeSnapshotTreeStatus status;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public void setRootImageUuid(String rootImageUuid) {
        this.rootImageUuid = rootImageUuid;
    }

    public String getRootImageUuid() {
        return rootImageUuid;
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

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }

    public VolumeSnapshotTreeStatus getStatus() {
        return status;
    }

    public void setStatus(VolumeSnapshotTreeStatus status) {
        this.status = status;
    }
}
