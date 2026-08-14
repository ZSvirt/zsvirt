package org.zstack.sdk;

import org.zstack.sdk.VHostAddOn;
import org.zstack.sdk.DeviceAddress;

public class NicTO extends org.zstack.sdk.BaseVirtualDeviceTO {

    public java.lang.String mac;
    public void setMac(java.lang.String mac) {
        this.mac = mac;
    }
    public java.lang.String getMac() {
        return this.mac;
    }

    public java.util.List ips;
    public void setIps(java.util.List ips) {
        this.ips = ips;
    }
    public java.util.List getIps() {
        return this.ips;
    }

    public java.lang.String bridgeName;
    public void setBridgeName(java.lang.String bridgeName) {
        this.bridgeName = bridgeName;
    }
    public java.lang.String getBridgeName() {
        return this.bridgeName;
    }

    public java.lang.String physicalInterface;
    public void setPhysicalInterface(java.lang.String physicalInterface) {
        this.physicalInterface = physicalInterface;
    }
    public java.lang.String getPhysicalInterface() {
        return this.physicalInterface;
    }

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String nicInternalName;
    public void setNicInternalName(java.lang.String nicInternalName) {
        this.nicInternalName = nicInternalName;
    }
    public java.lang.String getNicInternalName() {
        return this.nicInternalName;
    }

    public int deviceId;
    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }
    public int getDeviceId() {
        return this.deviceId;
    }

    public java.lang.String metaData;
    public void setMetaData(java.lang.String metaData) {
        this.metaData = metaData;
    }
    public java.lang.String getMetaData() {
        return this.metaData;
    }

    public java.lang.Boolean useVirtio;
    public void setUseVirtio(java.lang.Boolean useVirtio) {
        this.useVirtio = useVirtio;
    }
    public java.lang.Boolean getUseVirtio() {
        return this.useVirtio;
    }

    public int bootOrder;
    public void setBootOrder(int bootOrder) {
        this.bootOrder = bootOrder;
    }
    public int getBootOrder() {
        return this.bootOrder;
    }

    public java.lang.Integer mtu;
    public void setMtu(java.lang.Integer mtu) {
        this.mtu = mtu;
    }
    public java.lang.Integer getMtu() {
        return this.mtu;
    }

    public java.lang.String driverType;
    public void setDriverType(java.lang.String driverType) {
        this.driverType = driverType;
    }
    public java.lang.String getDriverType() {
        return this.driverType;
    }

    public VHostAddOn vHostAddOn;
    public void setVHostAddOn(VHostAddOn vHostAddOn) {
        this.vHostAddOn = vHostAddOn;
    }
    public VHostAddOn getVHostAddOn() {
        return this.vHostAddOn;
    }

    public DeviceAddress pci;
    public void setPci(DeviceAddress pci) {
        this.pci = pci;
    }
    public DeviceAddress getPci() {
        return this.pci;
    }

    public java.lang.String type;
    public void setType(java.lang.String type) {
        this.type = type;
    }
    public java.lang.String getType() {
        return this.type;
    }

    public java.lang.String state;
    public void setState(java.lang.String state) {
        this.state = state;
    }
    public java.lang.String getState() {
        return this.state;
    }

    public java.lang.String vlanId;
    public void setVlanId(java.lang.String vlanId) {
        this.vlanId = vlanId;
    }
    public java.lang.String getVlanId() {
        return this.vlanId;
    }

    public java.lang.String pciDeviceAddress;
    public void setPciDeviceAddress(java.lang.String pciDeviceAddress) {
        this.pciDeviceAddress = pciDeviceAddress;
    }
    public java.lang.String getPciDeviceAddress() {
        return this.pciDeviceAddress;
    }

    public java.lang.String ipForTf;
    public void setIpForTf(java.lang.String ipForTf) {
        this.ipForTf = ipForTf;
    }
    public java.lang.String getIpForTf() {
        return this.ipForTf;
    }

    public java.lang.String l2NetworkUuid;
    public void setL2NetworkUuid(java.lang.String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }
    public java.lang.String getL2NetworkUuid() {
        return this.l2NetworkUuid;
    }

    public java.lang.String srcPath;
    public void setSrcPath(java.lang.String srcPath) {
        this.srcPath = srcPath;
    }
    public java.lang.String getSrcPath() {
        return this.srcPath;
    }

    public java.lang.Boolean cleanTraffic;
    public void setCleanTraffic(java.lang.Boolean cleanTraffic) {
        this.cleanTraffic = cleanTraffic;
    }
    public java.lang.Boolean getCleanTraffic() {
        return this.cleanTraffic;
    }

}
