package org.zstack.storage.device.multipath;

import java.util.List;

public class MultipathTopologyStruct {
    private String lunUuid;
    private List<DeviceTO> devices;

    public String getLunUuid() {
        return lunUuid;
    }

    public void setLunUuid(String lunUuid) {
        this.lunUuid = lunUuid;
    }

    public List<DeviceTO> getDevices() {
        return devices;
    }

    public void setDevices(List<DeviceTO> devices) {
        this.devices = devices;
    }
}
