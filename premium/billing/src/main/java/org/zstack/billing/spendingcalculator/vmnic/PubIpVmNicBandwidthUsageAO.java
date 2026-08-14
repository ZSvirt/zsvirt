package org.zstack.billing.spendingcalculator.vmnic;

import org.zstack.billing.Usage;
import org.zstack.billing.UsageAO;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by lining on 2019/4/3.
 */

@MappedSuperclass
public class PubIpVmNicBandwidthUsageAO extends UsageAO implements Usage {
    @Id
    @Column
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private long id;

    @Column
    private String vmNicUuid;

    @Column
    private String vmInstanceUuid;

    @Column
    private Long bandwidthOut;

    @Column
    private Long bandwidthIn;

    @Column
    private String vmNicIp;

    @Column
    private String vmNicStatus;

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
        return vmNicUuid;
    }

    public String getInventory() {
        return inventory;
    }

    public void setInventory(String inventory) {
        this.inventory = inventory;
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

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getVmNicStatus() {
        return vmNicStatus;
    }

    public void setVmNicStatus(String vmNicStatus) {
        this.vmNicStatus = vmNicStatus;
    }

    public PubIpVmNicBandwidthUsageAO() {

    }

    public PubIpVmNicBandwidthUsageAO(PubIpVmNicBandwidthUsageAO other) {
        this.setId(other.getId());
        this.setAccountUuid(other.getAccountUuid());
        this.setDateInLong(other.getDateInLong());
        this.setBandwidthIn(other.getBandwidthIn());
        this.setBandwidthOut(other.getBandwidthOut());
        this.setCreateDate(other.getCreateDate());
        this.setInventory(other.getInventory());
        this.setLastOpDate(other.getLastOpDate());
        this.setL3NetworkUuid(other.getL3NetworkUuid());
        this.setVmInstanceUuid(other.getVmInstanceUuid());
        this.setVmNicIp(other.getVmNicIp());
        this.setVmNicUuid(other.getVmNicUuid());
        this.setVmNicStatus(other.getVmNicStatus());
    }
}
