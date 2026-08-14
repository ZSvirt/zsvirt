package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 * Created by frank on 11/8/2015.
 */
public class CheckVmStateOnHypervisorMsg extends NeedReplyMessage implements HostMessage {
    private List<String> vmInstanceUuids;
    private String hostUuid;

    public List<String> getVmInstanceUuids() {
        return vmInstanceUuids;
    }

    public void setVmInstanceUuids(List<String> vmInstanceUuids) {
        this.vmInstanceUuids = vmInstanceUuids;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
