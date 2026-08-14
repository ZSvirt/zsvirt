package org.zstack.billing.spendingcalculator.vip;

import org.zstack.billing.Usage;
import org.zstack.billing.UsageAO;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by lining on 2019/4/3.
 */

@MappedSuperclass
public class PubIpVipBandwidthUsageAO extends UsageAO implements Usage {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column
    private String vipUuid;

    @Column
    private String vipName;

    @Column
    private String vipIp;

    @Column
    private String vipStatus;

    @Column
    private Long bandwidthOut;

    @Column
    private Long bandwidthIn;

    @Column
    private String l3NetworkUuid;

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
        return vipUuid;
    }

    public String getInventory() {
        return inventory;
    }

    public void setInventory(String inventory) {
        this.inventory = inventory;
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

    public String getVipStatus() {
        return vipStatus;
    }

    public void setVipStatus(String vipStatus) {
        this.vipStatus = vipStatus;
    }

    public PubIpVipBandwidthUsageAO() {

    }

    public PubIpVipBandwidthUsageAO(PubIpVipBandwidthUsageAO other) {
        this.setId(other.getId());
        this.setAccountUuid(other.getAccountUuid());
        this.setDateInLong(other.getDateInLong());
        this.setBandwidthIn(other.getBandwidthIn());
        this.setBandwidthOut(other.getBandwidthOut());
        this.setCreateDate(other.getCreateDate());
        this.setInventory(other.getInventory());
        this.setLastOpDate(other.getLastOpDate());
        this.setL3NetworkUuid(other.getL3NetworkUuid());
        this.setVipIp(other.getVipIp());
        this.setVipName(other.getVipName());
        this.setVipStatus(other.getVipStatus());
        this.setVipUuid(other.getVipUuid());
    }

}
