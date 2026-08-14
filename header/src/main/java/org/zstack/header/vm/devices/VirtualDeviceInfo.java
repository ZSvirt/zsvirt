package org.zstack.header.vm.devices;

public class VirtualDeviceInfo {
    private String resourceUuid;
    private DeviceAddress deviceAddress;

    public VirtualDeviceInfo(String resourceUuid, DeviceAddress deviceAddress) {
        this.resourceUuid = resourceUuid;
        this.deviceAddress = deviceAddress;
    }

    public VirtualDeviceInfo() {
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public DeviceAddress getDeviceAddress() {
        return deviceAddress;
    }

    public void setDeviceAddress(DeviceAddress deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public boolean isValid() {
        return resourceUuid != null && deviceAddress != null;
    }
}
