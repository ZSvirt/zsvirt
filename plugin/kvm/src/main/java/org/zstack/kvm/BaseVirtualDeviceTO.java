package org.zstack.kvm;

import org.zstack.header.vm.devices.DeviceAddress;

public class BaseVirtualDeviceTO {
    protected String resourceUuid;
    protected DeviceAddress deviceAddress;

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
}
