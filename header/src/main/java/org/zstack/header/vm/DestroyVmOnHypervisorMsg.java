package org.zstack.header.vm;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

public class DestroyVmOnHypervisorMsg extends NeedReplyMessage implements HostMessage {
    private VmInstanceInventory vmInventory;

    public VmInstanceInventory getVmInventory() {
        return vmInventory;
    }

    public void setVmInventory(VmInstanceInventory vmInventory) {
        this.vmInventory = vmInventory;
    }

    @Override
    public String getHostUuid() {
        if (vmInventory.getHostUuid() != null) {
            return vmInventory.getHostUuid();
        } else {
            return vmInventory.getLastHostUuid();
        }
    }
}
