package org.zstack.header.configuration;

import org.zstack.header.vo.Index;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;

@MappedSuperclass
public class InstanceOfferingAO extends ResourceVO {
    @Column
    @Index
    private String name;

    @Column
    private String description;

    @Column
    private int cpuNum;

    @Column
    private int cpuSpeed;

    @Column
    private long memorySize;

    @Column
    private long reservedMemorySize;

    @Column
    private String allocatorStrategy;

    @Column
    private int sortKey;

    @Column
    @Enumerated(EnumType.STRING)
    private InstanceOfferingState state;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @Column
    private String type;

    @Column
    @Enumerated(EnumType.STRING)
    private InstanceOfferingDuration duration;

    public InstanceOfferingAO(InstanceOfferingAO other) {
        this.uuid = other.uuid;
        this.name = other.name;
        this.description = other.description;
        this.cpuNum = other.cpuNum;
        this.cpuSpeed = other.cpuSpeed;
        this.memorySize = other.memorySize;
        this.allocatorStrategy = other.allocatorStrategy;
        this.sortKey = other.sortKey;
        this.state = other.state;
        this.createDate = other.createDate;
        this.lastOpDate = other.lastOpDate;
        this.type = other.type;
        this.duration = other.duration;
        this.reservedMemorySize = other.reservedMemorySize;
    }

    public InstanceOfferingAO() {
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public int getCpuNum() {
        return cpuNum;
    }

    public void setCpuNum(int cpuNum) {
        this.cpuNum = cpuNum;
    }

    public int getCpuSpeed() {
        return cpuSpeed;
    }

    public void setCpuSpeed(int cpuSpeed) {
        this.cpuSpeed = cpuSpeed;
    }

    public long getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(long memorySize) {
        this.memorySize = memorySize;
    }

    public String getAllocatorStrategy() {
        return allocatorStrategy;
    }

    public void setAllocatorStrategy(String allocatorStrategy) {
        this.allocatorStrategy = allocatorStrategy;
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

    public int getSortKey() {
        return sortKey;
    }

    public void setSortKey(int sortKey) {
        this.sortKey = sortKey;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public InstanceOfferingDuration getDuration() {
        return duration;
    }

    public void setDuration(InstanceOfferingDuration duration) {
        this.duration = duration;
    }

    public InstanceOfferingState getState() {
        return state;
    }

    public void setState(InstanceOfferingState state) {
        this.state = state;
    }

    public long getReservedMemorySize() {
        return reservedMemorySize;
    }

    public void setReservedMemorySize(long reservedMemorySize) {
        this.reservedMemorySize = reservedMemorySize;
    }
}
