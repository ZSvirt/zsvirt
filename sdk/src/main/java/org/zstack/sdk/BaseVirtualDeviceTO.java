package org.zstack.sdk;

import org.zstack.sdk.DeviceAddress;

public class BaseVirtualDeviceTO  {

    public java.lang.String resourceUuid;
    public void setResourceUuid(java.lang.String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }
    public java.lang.String getResourceUuid() {
        return this.resourceUuid;
    }

    public DeviceAddress deviceAddress;
    public void setDeviceAddress(DeviceAddress deviceAddress) {
        this.deviceAddress = deviceAddress;
    }
    public DeviceAddress getDeviceAddress() {
        return this.deviceAddress;
    }

}
