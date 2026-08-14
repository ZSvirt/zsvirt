package org.zstack.billing.spendingcalculator.volume.root;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by xing5 on 2016/9/15.
 */
@Inventory(mappingVOClass = RootVolumeUsageVO.class)
public class RootVolumeUsageInventory {
    private Long id;
    private String accountUuid;
    private Long dateInLong;
    private String vmUuid;
    private String volumeUuid;
    private String volumeStatus;
    private Long volumeName;
    private Long volumeSize;
    private String inventory;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static RootVolumeUsageInventory valueOf(RootVolumeUsageVO vo) {
        RootVolumeUsageInventory inv = new RootVolumeUsageInventory();
        inv.setCreateDate(vo.getCreateDate());
        inv.setDateInLong(vo.getDateInLong());
        inv.setAccountUuid(vo.getAccountUuid());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setId(vo.getId());
        inv.setInventory(vo.getInventory());
        inv.setVolumeSize(vo.getVolumeSize());
        inv.setVolumeStatus(vo.getVolumeStatus());
        inv.setVolumeUuid(vo.getVolumeUuid());
        inv.setVmUuid(vo.getVmUuid());
        return inv;
    }

    public static List<RootVolumeUsageInventory> valueOf(Collection<RootVolumeUsageVO> vos) {
        return vos.stream().map(RootVolumeUsageInventory::valueOf).collect(Collectors.toList());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public Long getDateInLong() {
        return dateInLong;
    }

    public void setDateInLong(Long dateInLong) {
        this.dateInLong = dateInLong;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
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

    public Long getVolumeName() {
        return volumeName;
    }

    public void setVolumeName(Long volumeName) {
        this.volumeName = volumeName;
    }

    public Long getVolumeSize() {
        return volumeSize;
    }

    public void setVolumeSize(Long volumeSize) {
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
