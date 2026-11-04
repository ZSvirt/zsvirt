package org.zstack.guesttools;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmInstanceMessage;

public class GetVmZWatchStatusMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String vmInstanceUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
