package org.zstack.header.vdpa;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmInstanceMessage;
import org.zstack.header.vm.VmNicInventory;

public class DeleteVdpasMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String vmInstanceUuid;
    private VmNicInventory nic;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public VmNicInventory getNic() {
        return nic;
    }

    public void setNic(VmNicInventory nic) {
        this.nic = nic;
    }
}
