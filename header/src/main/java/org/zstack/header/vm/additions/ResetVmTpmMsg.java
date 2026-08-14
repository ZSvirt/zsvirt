package org.zstack.header.vm.additions;

import org.zstack.header.message.NeedReplyMessage;

public class ResetVmTpmMsg extends NeedReplyMessage {
    private String vmInstanceUuid;

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
