package org.zstack.pciDevice.message;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class SyncPciDeviceInDbMsg extends NeedReplyMessage {
    public enum SyncMode {
        Full,
        // Specified, ZSphere not support
        Ethernet
    }

    private String hostUuid;
    private List<PciDeviceTO> pciDevices;
    private List<String> pciDeviceAddresses;
    private SyncMode syncMode;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public List<PciDeviceTO> getPciDevices() {
        return pciDevices;
    }

    public void setPciDevices(List<PciDeviceTO> pciDevices) {
        this.pciDevices = pciDevices;
    }

    public List<String> getPciDeviceAddresses() {
        return pciDeviceAddresses;
    }

    public void setPciDeviceAddresses(List<String> pciDeviceAddresses) {
        this.pciDeviceAddresses = pciDeviceAddresses;
    }

    public SyncMode getSyncMode() {
        return syncMode;
    }

    public void setSyncMode(SyncMode syncMode) {
        this.syncMode = syncMode;
    }
}