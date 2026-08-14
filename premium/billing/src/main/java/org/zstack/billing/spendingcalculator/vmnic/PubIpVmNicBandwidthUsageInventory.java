package org.zstack.billing.spendingcalculator.vmnic;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by xing5 on 2016/9/15.
 */
@Inventory(mappingVOClass = PubIpVmNicBandwidthUsageVO.class)
public class PubIpVmNicBandwidthUsageInventory {
    private Long id;

    private String accountUuid;

    private Long dateInLong;

    private String name;

    private String inventory;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    private String vmNicUuid;

    private String vmInstanceUuid;

    private Long bandwidthOut;

    private Long bandwidthIn;

    private String vmNicIp;

    private String vmNicStatus;

    private String l3NetworkUuid;

    public static PubIpVmNicBandwidthUsageInventory valueOf(PubIpVmNicBandwidthUsageVO vo) {
        PubIpVmNicBandwidthUsageInventory inv = new PubIpVmNicBandwidthUsageInventory();
        inv.setId(vo.getId());
        inv.setAccountUuid(vo.getAccountUuid());
        inv.setDateInLong(vo.getDateInLong());
        inv.setInventory(vo.getInventory());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setVmNicStatus(vo.getVmNicStatus());
        inv.setVmNicUuid(vo.getVmNicUuid());
        inv.setBandwidthIn(vo.getBandwidthIn());
        inv.setBandwidthOut(vo.getBandwidthOut());
        inv.setL3NetworkUuid(vo.getL3NetworkUuid());
        inv.setVmInstanceUuid(vo.getVmInstanceUuid());
        inv.setVmNicIp(vo.getVmNicIp());
        return inv;
    }

    public static List<PubIpVmNicBandwidthUsageInventory> valueOf(Collection<PubIpVmNicBandwidthUsageVO> vos) {
        return vos.stream().map(PubIpVmNicBandwidthUsageInventory::valueOf).collect(Collectors.toList());
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public Long getBandwidthOut() {
        return bandwidthOut;
    }

    public void setBandwidthOut(Long bandwidthOut) {
        this.bandwidthOut = bandwidthOut;
    }

    public Long getBandwidthIn() {
        return bandwidthIn;
    }

    public void setBandwidthIn(Long bandwidthIn) {
        this.bandwidthIn = bandwidthIn;
    }

    public String getVmNicIp() {
        return vmNicIp;
    }

    public void setVmNicIp(String vmNicIp) {
        this.vmNicIp = vmNicIp;
    }

    public String getVmNicStatus() {
        return vmNicStatus;
    }

    public void setVmNicStatus(String vmNicStatus) {
        this.vmNicStatus = vmNicStatus;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }
}
