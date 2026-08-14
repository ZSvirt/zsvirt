package org.zstack.billing.spendingcalculator.volume.root;

import org.zstack.billing.Usage;
import org.zstack.billing.UsageAO;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by lining on 2019/4/2.
 */

@MappedSuperclass
public class RootVolumeUsageAO extends UsageAO implements Usage {
    @Id
    @Column
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private long id;
    @Column
    private String vmUuid;
    @Column
    private String volumeUuid;
    @Column
    private String volumeStatus;
    @Column
    private String volumeName;
    @Column
    private long volumeSize;
    @Column
    private String inventory;
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

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    @Override
    public String getUsageId() {
        return volumeUuid;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getVolumeStatus() {
        return volumeStatus;
    }

    public void setVolumeStatus(String volumeStatus) {
        this.volumeStatus = volumeStatus;
    }

    public String getVolumeName() {
        return volumeName;
    }

    public void setVolumeName(String volumeName) {
        this.volumeName = volumeName;
    }

    public long getVolumeSize() {
        return volumeSize;
    }

    public void setVolumeSize(long volumeSize) {
        this.volumeSize = volumeSize;
    }

    public String getInventory() {
        return inventory;
    }

    public void setInventory(String inventory) {
        this.inventory = inventory;
    }

    public RootVolumeUsageAO() {

    }

    public RootVolumeUsageAO(RootVolumeUsageAO other) {
        this.id = other.getId();
        this.lastOpDate = other.getLastOpDate();
        this.dateInLong = other.getDateInLong();
        this.accountUuid = other.getAccountUuid();
        this.createDate = other.getCreateDate();
        this.inventory = other.getInventory();
        this.volumeName = other.getVolumeName();
        this.volumeSize = other.getVolumeSize();
        this.vmUuid = other.getVmUuid();
        this.volumeUuid = other.getVolumeUuid();
        this.volumeStatus = other.getVolumeStatus();
    }
}
