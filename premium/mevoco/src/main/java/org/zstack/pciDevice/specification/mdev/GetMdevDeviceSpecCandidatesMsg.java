package org.zstack.pciDevice.specification.mdev;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 * Created by GuoYi on 2019-05-22.
 */
public class GetMdevDeviceSpecCandidatesMsg extends NeedReplyMessage {
    private List<String> clusterUuids;
    private String hostUuid;
    private String vmInstanceUuid;
    private List<String> types;
    private SessionInventory session;

    public List<String> getClusterUuids() {
        return clusterUuids;
    }

    public void setClusterUuids(List<String> clusterUuids) {
        this.clusterUuids = clusterUuids;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        this.types = types;
    }

    public SessionInventory getSession() {
        return session;
    }

    public void setSession(SessionInventory session) {
        this.session = session;
    }
}
