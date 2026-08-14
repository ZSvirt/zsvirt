package org.zstack.header.storage.primary;

import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.zone.ZoneEO;

import javax.persistence.*;
import java.sql.Timestamp;

@MappedSuperclass
public class PrimaryStorageAO extends ResourceVO {
    @Column
    @ForeignKey(parentEntityClass = ZoneEO.class, onDeleteAction = ReferenceOption.RESTRICT)
    private String zoneUuid;

    @Column
    private String name;

    @Column
    private String url;

    @Column
    private String description;

    @Column
    private String type;

    @Column
    private String mountPath;

    @Column
    @Enumerated(EnumType.STRING)
    private PrimaryStorageState state;

    @Column
    @Enumerated(EnumType.STRING)
    private PrimaryStorageStatus status;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public PrimaryStorageAO() {
    }

    public PrimaryStorageAO(PrimaryStorageAO other) {
        this.uuid = other.uuid;
        this.zoneUuid = other.zoneUuid;
        this.name = other.name;
        this.url = other.url;
        this.description = other.description;
        this.type = other.type;
        this.mountPath = other.mountPath;
        this.state = other.state;
        this.status = other.status;
        this.createDate = other.createDate;
        this.lastOpDate = other.lastOpDate;
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public PrimaryStorageStatus getStatus() {
        return status;
    }

    public void setStatus(PrimaryStorageStatus status) {
        this.status = status;
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

    public PrimaryStorageState getState() {
        return state;
    }

    public void setState(PrimaryStorageState state) {
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMountPath() {
        return mountPath;
    }

    public void setMountPath(String mountPath) {
        this.mountPath = mountPath;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }
}
