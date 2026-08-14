package org.zstack.billing.spendingcalculator.volume.data;

import org.zstack.billing.Usage;
import org.zstack.billing.UsageAO;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by lining on 2019/4/3.
 */

@MappedSuperclass
public class DataVolumeUsageAO extends UsageAO implements Usage {
    @Id
    @Column
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private long id;

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

    public DataVolumeUsageAO() {
    }

    public DataVolumeUsageAO(DataVolumeUsageAO other) {
        this.setLastOpDate(other.getLastOpDate());
        this.setAccountUuid(other.getAccountUuid());
        this.setId(other.id);
        this.setDateInLong(other.getDateInLong());
        this.setCreateDate(other.getCreateDate());
        this.setInventory(other.getInventory());
        this.setVolumeSize(other.getVolumeSize());
        this.setVolumeStatus(other.getVolumeStatus());
        this.setVolumeName(other.getVolumeName());
        this.setVolumeUuid(other.getVolumeUuid());
    }
}
