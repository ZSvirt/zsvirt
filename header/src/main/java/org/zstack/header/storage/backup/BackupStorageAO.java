package org.zstack.header.storage.backup;

import org.zstack.header.vo.Index;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;

@MappedSuperclass
public class BackupStorageAO extends ResourceVO {
    @Column
    @Index
    private String name;

    @Column
    private String url;

    @Column
    private String description;

    @Column
    private long totalCapacity;

    @Column
    private long availableCapacity;

    @Column
    private String type;

    @Column
    @Enumerated(EnumType.STRING)
    private BackupStorageState state;

    @Column
    @Enumerated(EnumType.STRING)
    private BackupStorageStatus status;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public BackupStorageAO() {
    }

    public long getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(long availableCapacity) {
        this.availableCapacity = availableCapacity;
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

    public long getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BackupStorageState getState() {
        return state;
    }

    public void setState(BackupStorageState state) {
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

    public BackupStorageStatus getStatus() {
        return status;
    }

    public void setStatus(BackupStorageStatus status) {
        this.status = status;
    }

}
