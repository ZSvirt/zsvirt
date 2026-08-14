package org.zstack.billing.spendingcalculator.snapshot;

import org.zstack.billing.Usage;
import org.zstack.billing.UsageAO;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by camile on 5/16/2017.
 */
@Table(name = "SnapShotUsageVO")
@Entity
public class SnapShotUsageVO extends UsageAO implements Usage {
    @Id
    @Column
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private long id;
    @Column
    private String volumeUuid;
    @Column
    private String SnapshotUuid;
    @Column
    private String SnapshotStatus;
    @Column
    private String SnapshotName;
    @Column
    private long  SnapshotSize;
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


    @Override
    public String getUsageId() {
        return SnapshotUuid;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getSnapshotUuid() {
        return SnapshotUuid;
    }

    public void setSnapshotUuid(String snapshotUuid) {
        SnapshotUuid = snapshotUuid;
    }

    public String getSnapshotStatus() {
        return SnapshotStatus;
    }

    public void setSnapshotStatus(String snapshotStatus) {
        SnapshotStatus = snapshotStatus;
    }

    public String getSnapshotName() {
        return SnapshotName;
    }

    public void setSnapshotName(String snapshotName) {
        SnapshotName = snapshotName;
    }

    public long getSnapshotSize() {
        return SnapshotSize;
    }

    public void setSnapshotSize(long snapshotSize) {
        SnapshotSize = snapshotSize;
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

    public String getInventory() {
        return inventory;
    }

    public void setInventory(String inventory) {
        this.inventory = inventory;
    }
}
