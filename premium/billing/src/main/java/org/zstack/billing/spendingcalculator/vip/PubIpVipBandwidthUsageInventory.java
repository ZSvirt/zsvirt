package org.zstack.billing.spendingcalculator.vip;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by lining on 2018/11/20.
 */
@Inventory(mappingVOClass = PubIpVipBandwidthUsageVO.class)
public class PubIpVipBandwidthUsageInventory {
    private Long id;

    private String accountUuid;

    private Long dateInLong;

    private String name;

    private String inventory;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    private String vipUuid;

    private String vipName;

    private String vipIp;

    private Long bandwidthIn;

    private Long bandwidthOut;

    private String vipStatus;

    private String l3NetworkUuid;

    public static PubIpVipBandwidthUsageInventory valueOf(PubIpVipBandwidthUsageVO vo) {
        PubIpVipBandwidthUsageInventory inv = new PubIpVipBandwidthUsageInventory();
        inv.setId(vo.getId());
        inv.setAccountUuid(vo.getAccountUuid());
        inv.setDateInLong(vo.getDateInLong());
        inv.setInventory(vo.getInventory());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setBandwidthIn(vo.getBandwidthIn());
        inv.setBandwidthOut(vo.getBandwidthOut());
        inv.setL3NetworkUuid(vo.getL3NetworkUuid());
        inv.setVipStatus(vo.getVipStatus());
        inv.setVipName(vo.getVipName());
        inv.setVipUuid(vo.getVipUuid());
        inv.setVipIp(vo.getVipIp());
        return inv;
    }

    public static List<PubIpVipBandwidthUsageInventory> valueOf(Collection<PubIpVipBandwidthUsageVO> vos) {
        return vos.stream().map(PubIpVipBandwidthUsageInventory::valueOf).collect(Collectors.toList());
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

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }

    public String getVipName() {
        return vipName;
    }

    public void setVipName(String vipName) {
        this.vipName = vipName;
    }

    public String getVipIp() {
        return vipIp;
    }

    public void setVipIp(String vipIp) {
        this.vipIp = vipIp;
    }

    public String getVipStatus() {
        return vipStatus;
    }

    public void setVipStatus(String vipStatus) {
        this.vipStatus = vipStatus;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setDateInLong(Long dateInLong) {
        this.dateInLong = dateInLong;
    }
}
