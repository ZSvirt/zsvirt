package org.zstack.header.storage.primary;

import org.zstack.header.vo.Index;

import javax.persistence.*;
import java.sql.Timestamp;

@MappedSuperclass
public abstract class HistoricalUsageAO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    @Index
    protected String resourceType;

    @Column
    private long totalPhysicalCapacity;

    @Column
    private long usedPhysicalCapacity;

    @Column
    private Timestamp recordDate;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public HistoricalUsageAO() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public long getTotalPhysicalCapacity() {
        return totalPhysicalCapacity;
    }

    public void setTotalPhysicalCapacity(long totalPhysicalCapacity) {
        this.totalPhysicalCapacity = totalPhysicalCapacity;
    }

    public long getUsedPhysicalCapacity() {
        return usedPhysicalCapacity;
    }

    public void setUsedPhysicalCapacity(long usedPhysicalCapacity) {
        this.usedPhysicalCapacity = usedPhysicalCapacity;
    }

    public Timestamp getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(Timestamp recordDate) {
        this.recordDate = recordDate;
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

    public abstract String getResourceUuid();

    public abstract void setResourceUuid(String resourceUuid);
}
