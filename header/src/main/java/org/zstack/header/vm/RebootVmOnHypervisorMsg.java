package org.zstack.header.vm;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class RebootVmOnHypervisorMsg extends NeedReplyMessage implements HostMessage {
    private VmInstanceInventory vmInventory;
    private List<String> bootOrders;

    public List<String> getBootOrders() {
        return bootOrders;
    }

    public void setBootOrders(List<String> bootOrders) {
        this.bootOrders = bootOrders;
    }

    public VmInstanceInventory getVmInventory() {
        return vmInventory;
    }

    public void setVmInventory(VmInstanceInventory vmInventory) {
        this.vmInventory = vmInventory;
    }

    @Override
    public String getHostUuid() {
        return vmInventory.getHostUuid();
    }
}
