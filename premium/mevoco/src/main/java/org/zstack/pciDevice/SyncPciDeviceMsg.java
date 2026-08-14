package org.zstack.pciDevice;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 * Created by weiwang on 13/07/2017.
 */
public class SyncPciDeviceMsg extends NeedReplyMessage {
    String hostUuid;
    String hostIommuState;
    boolean skipGrubConfig;

    // sync info of specified pci devices
    List<String> pciDeviceAddresses;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getHostIommuState() {
        return hostIommuState;
    }

    public void setHostIommuState(String hostIommuState) {
        this.hostIommuState = hostIommuState;
    }

    public boolean isSkipGrubConfig() {
        return skipGrubConfig;
    }

    public void setSkipGrubConfig(boolean skipGrubConfig) {
        this.skipGrubConfig = skipGrubConfig;
    }

    public List<String> getPciDeviceAddresses() {
        return pciDeviceAddresses;
    }

    public void setPciDeviceAddresses(List<String> pciDeviceAddresses) {
        this.pciDeviceAddresses = pciDeviceAddresses;
    }
}
