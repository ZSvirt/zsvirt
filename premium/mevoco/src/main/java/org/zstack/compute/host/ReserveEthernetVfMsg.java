package org.zstack.compute.host;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.sriov.EthernetVfStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class ReserveEthernetVfMsg extends NeedReplyMessage implements HostMessage {
    private String vmUuid;

    private String hostUuid;

    private List<String> l3Uuids;

    private Map<String, String> l3PciDeviceMap = new HashMap<>();

    private boolean releaseOldVf;

    private EthernetVfStatus status = EthernetVfStatus.Reserved;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public List<String> getL3Uuids() {
        return l3Uuids;
    }

    public void setL3Uuids(List<String> l3Uuids) {
        this.l3Uuids = l3Uuids;
    }

    public Map<String, String> getL3PciDeviceMap() {
        return l3PciDeviceMap;
    }

    public void setL3PciDeviceMap(Map<String, String> l3PciDeviceMap) {
        this.l3PciDeviceMap = l3PciDeviceMap;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public boolean isReleaseOldVf() {
        return releaseOldVf;
    }

    public void setReleaseOldVf(boolean releaseOldVf) {
        this.releaseOldVf = releaseOldVf;
    }

    public EthernetVfStatus getStatus() {
        return status;
    }

    public void setStatus(EthernetVfStatus status) {
        this.status = status;
    }
}
