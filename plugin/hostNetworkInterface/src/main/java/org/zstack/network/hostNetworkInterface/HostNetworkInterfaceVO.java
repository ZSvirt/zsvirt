package org.zstack.network.hostNetworkInterface;

import org.zstack.header.host.HostEO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.Index;
import org.zstack.header.vo.*;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by GuoYi on 4/23/20.
 */
@Entity
@Table
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = HostEO.class, joinColumn = "hostUuid")
})
public class HostNetworkInterfaceVO extends ResourceVO implements ToInventory {
    @Index
    @Column
    @ForeignKey(parentEntityClass = HostEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String hostUuid;

    @Index
    @Column
    @ForeignKey(parentEntityClass = HostNetworkBondingVO.class, onDeleteAction = ForeignKey.ReferenceOption.SET_NULL)
    private String bondingUuid;

    @Column
    private String interfaceModel;

    @Column
    private String vendorId;

    @Column
    private String deviceId;

    @Column
    private String deviceName;

    @Column
    private String vendorName;

    @Column
    private String subvendorId;

    @Column
    private String subdeviceId;

    @Column
    private String subvendorName;

    @Column
    private String interfaceName;

    @Column
    private String interfaceType;

    @Column
    private Long speed;

    @Column
    private boolean slaveActive;

    @Column
    private boolean carrierActive;

    @Column
    private String ipAddresses;

    @Column
    private String gateway;

    @Column
    private String mac;

    @Column
    private String callBackIp;

    @Column
    private String pciDeviceAddress;

    @Column
    private String offloadStatus;

    @Column
    private String virtStatus;

    @Column
    private String description;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getBondingUuid() {
        return bondingUuid;
    }

    public void setBondingUuid(String bondingUuid) {
        this.bondingUuid = bondingUuid;
    }

    public String getInterfaceModel() {
        return interfaceModel;
    }

    public void setInterfaceModel(String interfaceModel) {
        this.interfaceModel = interfaceModel;
    }

    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public String getSubvendorId() {
        return subvendorId;
    }

    public void setSubvendorId(String subvendorId) {
        this.subvendorId = subvendorId;
    }

    public String getSubdeviceId() {
        return subdeviceId;
    }

    public void setSubdeviceId(String subdeviceId) {
        this.subdeviceId = subdeviceId;
    }

    public String getSubvendorName() {
        return subvendorName;
    }

    public void setSubvendorName(String subvendorName) {
        this.subvendorName = subvendorName;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getInterfaceType() {
        return interfaceType;
    }

    public void setInterfaceType(String interfaceType) {
        this.interfaceType = interfaceType;
    }

    public Long getSpeed() {
        return speed;
    }

    public void setSpeed(Long speed) {
        this.speed = speed;
    }

    public boolean isSlaveActive() {
        return slaveActive;
    }

    public void setSlaveActive(boolean slaveActive) {
        this.slaveActive = slaveActive;
    }

    public boolean isCarrierActive() {
        return carrierActive;
    }

    public void setCarrierActive(boolean carrierActive) {
        this.carrierActive = carrierActive;
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

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getCallBackIp() {
        return callBackIp;
    }

    public void setCallBackIp(String callBackIp) {
        this.callBackIp = callBackIp;
    }

    public String getPciDeviceAddress() {
        return pciDeviceAddress;
    }

    public void setPciDeviceAddress(String pciDeviceAddress) {
        this.pciDeviceAddress = pciDeviceAddress;
    }

    public String getOffloadStatus() {
        return offloadStatus;
    }

    public void setOffloadStatus(String offloadStatus) {
        this.offloadStatus = offloadStatus;
    }

    public String getVirtStatus() {
        return virtStatus;
    }

    public void setVirtStatus(String virtStatus) {
        this.virtStatus = virtStatus;
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

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

}
