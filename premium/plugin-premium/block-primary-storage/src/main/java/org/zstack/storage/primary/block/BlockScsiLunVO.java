package org.zstack.storage.primary.block;

import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ToInventory;
import org.zstack.header.volume.VolumeVO;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/4/7 15:04
 */
@Entity
@Table
@AutoDeleteTag
public class BlockScsiLunVO implements ToInventory {
    @Column
    @Id
    private String uuid;

    @Column
    private String name;

    @Column
    private String wwn;

    @Column
    private Long size;

    @Column
    private Integer id;

    @Column
    @ForeignKey(parentEntityClass = VolumeVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String volumeUuid;

    @Column
    private String target;

    @Column
    private Integer lunMapId;

    @Column
    private String lunType;

    @Column
    private Integer lunInitSnapshotID;

    @Column
    private long usedSize;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

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

    public void setSize(Long size) {
        this.size = size;
    }

    public Long getSize() {
        return size;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setWwn(String wwn) {
        this.wwn = wwn;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public void setLunMapId(Integer lunMapId) {
        this.lunMapId = lunMapId;
    }

    public void setLunInitSnapshotID(Integer initSnapshotID) {
        this.lunInitSnapshotID = initSnapshotID;
    }

    public String getLunType() {
        return lunType;
    }

    public void setLunType(String lunType) {
        this.lunType = lunType;
    }

    public Integer getLunInitSnapshotID() {
        return lunInitSnapshotID;
    }

    public String getInstallPath() {
        String installPath = BlockPrimaryStorageConstants.BLOCK_INSTALL_PATH_SCHEME + getWwn();
        return installPath;
    }

    public String getWwn() {
        return wwn;
    }

    public Integer getLunMapId() {
        return lunMapId;
    }

    public long getUsedSize() {
        return usedSize;
    }

    public void setUsedSize(long usedSize) {
        this.usedSize = usedSize;
    }

}