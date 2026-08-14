package org.zstack.appliancevm;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmInstanceMessage;

public class ApplianceVmChangeStatusMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String vmInstanceUuid;
    private ApplianceVmStatus newStatus;

    public ApplianceVmStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(ApplianceVmStatus newStatus) {
        this.newStatus = newStatus;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}

