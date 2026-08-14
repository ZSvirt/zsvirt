package org.zstack.billing.spendingcalculator.vm;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by xing5 on 2016/9/15.
 */
@Inventory(mappingVOClass = VmUsageVO.class)
public class VmUsageInventory {
    private Long id;
    private String accountUuid;
    private Long dateInLong;
    private String vmUuid;
    private String state;
    private String name;
    private Integer cpuNum;
    private Long memorySize;
    private Long rootVolumeSize;
    private String inventory;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static VmUsageInventory valueOf(VmUsageVO vo) {
        VmUsageInventory inv = new VmUsageInventory();
        inv.setId(vo.getId());
        inv.setAccountUuid(vo.getAccountUuid());
        inv.setDateInLong(vo.getDateInLong());
        inv.setVmUuid(vo.getVmUuid());
        inv.setState(vo.getState());
        inv.setName(vo.getName());
        inv.setCpuNum(vo.getCpuNum());
        inv.setMemorySize(vo.getMemorySize());
        inv.setRootVolumeSize(vo.getRootVolumeSize());
        inv.setInventory(vo.getInventory());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<VmUsageInventory> valueOf(Collection<VmUsageVO> vos) {
        return vos.stream().map(VmUsageInventory::valueOf).collect(Collectors.toList());
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public long getRootVolumeSize() {
        return rootVolumeSize;
    }

    public void setRootVolumeSize(long rootVolumeSize) {
        this.rootVolumeSize = rootVolumeSize;
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
