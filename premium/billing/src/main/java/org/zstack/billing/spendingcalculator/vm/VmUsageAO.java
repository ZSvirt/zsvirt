package org.zstack.billing.spendingcalculator.vm;

import org.zstack.billing.Usage;
import org.zstack.billing.UsageAO;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by lining on 2019/3/29.
 */
@MappedSuperclass
public class VmUsageAO extends UsageAO implements Usage {
    @Id
    @Column
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private long id;
    @Column
    private String vmUuid;
    @Column
    private String state;
    @Column
    private String name;
    @Column
    private int cpuNum;
    @Column
    private long memorySize;
    @Column
    private long rootVolumeSize;
    @Column
    private String inventory;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;

    public VmUsageAO() {

    }

    public VmUsageAO(VmUsageAO other) {
        this.id = other.id;
        this.accountUuid = other.accountUuid;
        this.dateInLong = other.dateInLong;
        this.cpuNum = other.cpuNum;
        this.memorySize = other.memorySize;
        this.createDate = other.createDate;
        this.lastOpDate = other.lastOpDate;
        this.rootVolumeSize = other.rootVolumeSize;
        this.inventory = other.inventory;
        this.vmUuid = other.vmUuid;
        this.name = other.name;
        this.state = other.state;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public long getRootVolumeSize() {
        return rootVolumeSize;
    }

    public void setRootVolumeSize(long rootVolumeSize) {
        this.rootVolumeSize = rootVolumeSize;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getUsageId() {
        return vmUuid;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public int getCpuNum() {
        return cpuNum;
    }

    public void setCpuNum(int cpuNum) {
        this.cpuNum = cpuNum;
    }

    public long getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(long memorySize) {
        this.memorySize = memorySize;
    }

    public String getInventory() {
        return inventory;
    }

    public void setInventory(String inventory) {
        this.inventory = inventory;
    }
}
