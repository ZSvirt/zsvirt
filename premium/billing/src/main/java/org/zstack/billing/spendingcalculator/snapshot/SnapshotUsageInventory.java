package org.zstack.billing.spendingcalculator.snapshot;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by camile on 2016/5/18.
 */
@Inventory(mappingVOClass = SnapShotUsageVO.class)
public class SnapshotUsageInventory {
    private long id;
    private String volumeUuid;
    private String SnapshotUuid;
    private String SnapshotStatus;
    private String SnapshotName;
    private long  SnapshotSize;
    private String inventory;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static SnapshotUsageInventory valueOf(SnapShotUsageVO vo) {
        SnapshotUsageInventory inv = new SnapshotUsageInventory();
        inv.setId(vo.getId());
        inv.setVolumeUuid(vo.getVolumeUuid());
        inv.setSnapshotUuid(vo.getSnapshotUuid());
        inv.setSnapshotStatus(vo.getSnapshotStatus());
        inv.setSnapshotName(vo.getSnapshotName());
        inv.setSnapshotSize(vo.getSnapshotSize());
        inv.setInventory(vo.getInventory());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<SnapshotUsageInventory> valueOf(Collection<SnapShotUsageVO> vos) {
        return vos.stream().map(SnapshotUsageInventory::valueOf).collect(Collectors.toList());
    }

    public SnapshotUsageInventory() {

    }

    public SnapshotUsageInventory(long id, String volumeUuid, String snapshotUuid, String snapshotStatus, String snapshotName, long snapshotSize, String inventory, Timestamp createDate, Timestamp lastOpDate) {
        this.id = id;
        this.volumeUuid = volumeUuid;
        this.SnapshotUuid = snapshotUuid;
        this.SnapshotStatus = snapshotStatus;
        this.SnapshotName = snapshotName;
        this.SnapshotSize = snapshotSize;
        this.inventory = inventory;
        this.createDate = createDate;
        this.lastOpDate = lastOpDate;
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

    public String getInventory() {
        return inventory;
    }

    public void setInventory(String inventory) {
        this.inventory = inventory;
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
