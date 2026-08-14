package org.zstack.billing.spendingcalculator.volume.data;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by xing5 on 2016/9/15.
 */
@Inventory(mappingVOClass = DataVolumeUsageVO.class)
public class DataVolumeUsageInventory {
    private Long id;
    private String accountUuid;
    private Long dateInLong;
    private String volumeUuid;
    private String volumeStatus;
    private String volumeName;
    private Long volumeSize;
    private String inventory;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static DataVolumeUsageInventory valueOf(DataVolumeUsageVO vo) {
        DataVolumeUsageInventory inv = new DataVolumeUsageInventory();
        inv.setCreateDate(vo.getCreateDate());
        inv.setDateInLong(vo.getDateInLong());
        inv.setAccountUuid(vo.getAccountUuid());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setId(vo.getId());
        inv.setInventory(vo.getInventory());
        inv.setVolumeName(vo.getVolumeName());
        inv.setVolumeSize(vo.getVolumeSize());
        inv.setVolumeStatus(vo.getVolumeStatus());
        inv.setVolumeUuid(vo.getVolumeUuid());
        return inv;
    }

    public static List<DataVolumeUsageInventory> valueOf(Collection<DataVolumeUsageVO> vos) {
        return vos.stream().map(DataVolumeUsageInventory::valueOf).collect(Collectors.toList());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public long getDateInLong() {
        return dateInLong;
    }

    public void setDateInLong(long dateInLong) {
        this.dateInLong = dateInLong;
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
