package org.zstack.header.vm;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

/**
 */
public class VmAttachNicOnHypervisorMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    private VmNicInventory nicInventory;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public VmNicInventory getNicInventory() {
        return nicInventory;
    }

    public void setNicInventory(VmNicInventory nicInventory) {
        this.nicInventory = nicInventory;
    }
}
