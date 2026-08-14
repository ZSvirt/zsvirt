package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class GetVirtualizerInfoMsg extends NeedReplyMessage implements HostMessage {
    private List<String> vmInstanceUuids;
    private String hostUuid;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public List<String> getVmInstanceUuids() {
        return vmInstanceUuids;
    }

    public void setVmInstanceUuids(List<String> vmInstanceUuids) {
        this.vmInstanceUuids = vmInstanceUuids;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
