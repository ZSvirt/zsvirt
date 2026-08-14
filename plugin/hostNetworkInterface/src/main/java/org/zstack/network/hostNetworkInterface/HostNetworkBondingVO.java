package org.zstack.network.hostNetworkInterface;

import org.zstack.header.host.HostEO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.Index;
import org.zstack.header.vo.*;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by GuoYi on 4/23/20.
 */

@Entity
@Table
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = HostEO.class, joinColumn = "hostUuid")
})
public class HostNetworkBondingVO extends ResourceVO implements ToInventory {
    @Index
    @Column
    @ForeignKey(parentEntityClass = HostEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String hostUuid;

    @Column
    private String bondingName;

    @Column
    private String bondingType;

    @Column
    private Long speed;

    @Column
    private String mode;

    @Column
    private String xmitHashPolicy;

    @Column
    private String miiStatus;

    @Column
    private String mac;

    @Column
    private String ipAddresses;

    @Column
    private String gateway;

    @Column
    private String callBackIp;

    @Column
    private long miimon;

    @Column
    private String type;

    @Column
    private boolean allSlavesActive;

    @Column
    private String description;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @OneToMany(fetch= FetchType.EAGER)
    @JoinColumn(name="bondingUuid", insertable=false, updatable=false)
    @NoView
    private Set<HostNetworkInterfaceVO> slaves = new HashSet<>();

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getBondingName() {
        return bondingName;
    }

    public void setBondingName(String bondingName) {
        this.bondingName = bondingName;
    }

    public String getBondingType() {
        return bondingType;
    }

    public void setBondingType(String bondingType) {
        this.bondingType = bondingType;
    }

    public Long getSpeed() {
        return speed;
    }

    public void setSpeed(Long speed) {
        this.speed = speed;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getXmitHashPolicy() {
        return xmitHashPolicy;
    }

    public void setXmitHashPolicy(String xmitHashPolicy) {
        this.xmitHashPolicy = xmitHashPolicy;
    }

    public String getMiiStatus() {
        return miiStatus;
    }

    public void setMiiStatus(String miiStatus) {
        this.miiStatus = miiStatus;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getIpAddresses() {
        return ipAddresses;
    }

    public void setIpAddresses(String ipAddresses) {
        this.ipAddresses = ipAddresses;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getCallBackIp() {
        return callBackIp;
    }

    public void setCallBackIp(String callBackIp) {
        this.callBackIp = callBackIp;
    }

    public long getMiimon() {
        return miimon;
    }

    public void setMiimon(long miimon) {
        this.miimon = miimon;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isAllSlavesActive() {
        return allSlavesActive;
    }

    public void setAllSlavesActive(boolean allSlavesActive) {
        this.allSlavesActive = allSlavesActive;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Set<HostNetworkInterfaceVO> getSlaves() {
        return slaves;
    }

    public void setSlaves(Set<HostNetworkInterfaceVO> slaves) {
        this.slaves = slaves;
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }
}
