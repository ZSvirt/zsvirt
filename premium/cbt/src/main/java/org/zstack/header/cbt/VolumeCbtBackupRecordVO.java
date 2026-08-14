package org.zstack.header.cbt;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table
public class VolumeCbtBackupRecordVO implements Serializable {
    @Column
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column
    private String taskUuid;

    @Column
    private String volumeUuid;

    @Column
    private String mode;

    @Column
    private String target;

    @Column
    private String scratchNodeName;

    @Column
    private String bitmapName;

    @Column
    private String lastBitmapName;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTaskUuid() {
        return taskUuid;
    }

    public void setTaskUuid(String taskUuid) {
        this.taskUuid = taskUuid;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getScratchNodeName() {
        return scratchNodeName;
    }

    public void setScratchNodeName(String scratchNodeName) {
        this.scratchNodeName = scratchNodeName;
    }

    public String getBitmapName() {
        return bitmapName;
    }

    public void setBitmapName(String bitmapName) {
        this.bitmapName = bitmapName;
    }

    public String getLastBitmapName() {
        return lastBitmapName;
    }

    public void setLastBitmapName(String lastBitmapName) {
        this.lastBitmapName = lastBitmapName;
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
