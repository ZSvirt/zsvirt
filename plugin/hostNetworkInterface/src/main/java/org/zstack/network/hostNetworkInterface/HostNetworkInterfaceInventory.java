package org.zstack.network.hostNetworkInterface;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

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
@Inventory(mappingVOClass = HostNetworkInterfaceVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "bonding", inventoryClass = HostNetworkBondingInventory.class,
                foreignKey = "bondingUuid", expandedInventoryKey = "uuid"),
})
public class HostNetworkInterfaceInventory implements Serializable {
    private String uuid;
    private String hostUuid;
    private String bondingUuid;
    private String interfaceModel;
    private String vendorId;
    private String deviceId;
    private String deviceName;
    private String vendorName;
    private String subvendorId;
    private String subdeviceId;
    private String subvendorName;
    private String interfaceName;
    private String interfaceType;
    private Long speed;
    private Boolean slaveActive;
    private Boolean carrierActive;
    private List<String> ipAddresses;
    private String gateway;
    private String mac;
    private String callBackIp;
    private String pciDeviceAddress;
    private String offloadStatus;
    private String virtStatus;
    private String description;
    private Timestamp createDate;
    private Timestamp lastOpDate;

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

    public Boolean getSlaveActive() {
        return slaveActive;
    }

    public void setSlaveActive(Boolean slaveActive) {
        this.slaveActive = slaveActive;
    }

    public Boolean getCarrierActive() {
        return carrierActive;
    }

    public void setCarrierActive(Boolean carrierActive) {
        this.carrierActive = carrierActive;
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

    public HostNetworkInterfaceInventory() {

    }

    public HostNetworkInterfaceInventory(HostNetworkInterfaceVO vo) {
        this.uuid = vo.getUuid();
        this.hostUuid = vo.getHostUuid();
        this.bondingUuid = vo.getBondingUuid();
        this.interfaceModel = vo.getInterfaceModel();
        this.vendorId = vo.getVendorId();
        this.deviceId = vo.getDeviceId();
        this.deviceName = vo.getDeviceName();
        this.vendorName = vo.getVendorName();
        this.subvendorId = vo.getSubvendorId();
        this.subdeviceId = vo.getSubdeviceId();
        this.subvendorName = vo.getSubvendorName();
        this.interfaceName = vo.getInterfaceName();
        this.interfaceType = vo.getInterfaceType();
        this.mac = vo.getMac();
        this.pciDeviceAddress = vo.getPciDeviceAddress();
        if (vo.getIpAddresses() != null) {
            this.ipAddresses = Arrays.asList(vo.getIpAddresses().split(","));
        }
        this.gateway = vo.getGateway();
        this.speed = vo.getSpeed();
        this.callBackIp = vo.getCallBackIp();
        this.slaveActive = vo.isSlaveActive();
        this.carrierActive = vo.isCarrierActive();
        this.offloadStatus = vo.getOffloadStatus();
        this.virtStatus = vo.getVirtStatus();
        this.description = vo.getDescription();
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();
    }

    public static HostNetworkInterfaceInventory valueOf(HostNetworkInterfaceVO vo) {
        return new HostNetworkInterfaceInventory(vo);
    }

    public static List<HostNetworkInterfaceInventory> valueOf(Collection<HostNetworkInterfaceVO> vos) {
        return vos.stream().map(HostNetworkInterfaceInventory::valueOf).collect(Collectors.toList());
    }
}
