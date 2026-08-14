package org.zstack.network.hostNetworkInterface;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.Queryable;
import org.zstack.header.search.Inventory;

import javax.persistence.JoinColumn;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Create by weiwang at 2019-04-25
 */
@PythonClassInventory
@Inventory(mappingVOClass = HostNetworkBondingVO.class)
public class HostNetworkBondingInventory implements Serializable {
    private String uuid;
    private String hostUuid;
    private String bondingName;
    private String bondingType;
    private Long speed;
    private String mode;
    private String xmitHashPolicy;
    private String miiStatus;
    private String mac;
    private List<String> ipAddresses;
    private String gateway;
    private String callBackIp;
    private Long miimon;
    private String type;
    private Boolean allSlavesActive;
    private String description;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    @Queryable(mappingClass = HostNetworkInterfaceInventory.class, joinColumn = @JoinColumn(name = "bondingUuid"))
    private List<HostNetworkInterfaceInventory> slaves;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

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

    public Long getMiimon() {
        return miimon;
    }

    public void setMiimon(Long miimon) {
        this.miimon = miimon;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public List<String> getIpAddresses() {
        return ipAddresses;
    }

    public void setIpAddresses(List<String> ipAddresses) {
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

    public Boolean getAllSlavesActive() {
        return allSlavesActive;
    }

    public void setAllSlavesActive(Boolean allSlavesActive) {
        this.allSlavesActive = allSlavesActive;
    }

    public List<HostNetworkInterfaceInventory> getSlaves() {
        return slaves;
    }

    public void setSlaves(List<HostNetworkInterfaceInventory> slaves) {
        this.slaves = slaves;
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

    public HostNetworkBondingInventory() {

    }

    public HostNetworkBondingInventory(HostNetworkBondingVO vo) {
        this.uuid = vo.getUuid();
        this.hostUuid = vo.getHostUuid();
        this.bondingName = vo.getBondingName();
        this.bondingType = vo.getBondingType();
        this.speed = vo.getSpeed();
        this.callBackIp = vo.getCallBackIp();
        this.mode = vo.getMode();
        this.mac = vo.getMac();
        this.miimon = vo.getMiimon();
        this.miiStatus = vo.getMiiStatus();
        this.xmitHashPolicy = vo.getXmitHashPolicy();
        this.allSlavesActive = vo.isAllSlavesActive();
        this.type = vo.getType();
        this.slaves = HostNetworkInterfaceInventory.valueOf(vo.getSlaves());
        this.description = vo.getDescription();
        if (vo.getIpAddresses() != null) {
            this.ipAddresses = Arrays.asList(vo.getIpAddresses().split(","));
        }
        this.gateway = vo.getGateway();
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();
    }

    public static HostNetworkBondingInventory valueOf(HostNetworkBondingVO vo) {
        return new HostNetworkBondingInventory(vo);
    }

    public static List<HostNetworkBondingInventory> valueOf(Collection<HostNetworkBondingVO> vos) {
        return vos.stream().map(HostNetworkBondingInventory::valueOf).collect(Collectors.toList());
    }
}
