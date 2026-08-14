package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class AttachNicToVmMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String vmInstanceUuid;
    private List<VmNicInventory> nics;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public List<VmNicInventory> getNics() {
        return nics;
    }

    public void setNics(List<VmNicInventory> nics) {
        this.nics = nics;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

}
