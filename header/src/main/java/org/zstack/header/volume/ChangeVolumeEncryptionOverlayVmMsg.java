package org.zstack.header.volume;

import org.zstack.header.message.OverlayMessage;
import org.zstack.header.vm.VmInstanceMessage;

public class ChangeVolumeEncryptionOverlayVmMsg extends OverlayMessage implements VmInstanceMessage {
    private String vmInstanceUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
