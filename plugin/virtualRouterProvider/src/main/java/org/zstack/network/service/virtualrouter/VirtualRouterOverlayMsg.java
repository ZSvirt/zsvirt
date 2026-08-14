package org.zstack.network.service.virtualrouter;

import org.zstack.header.message.OverlayMessage;
import org.zstack.header.vm.VmInstanceMessage;

public class VirtualRouterOverlayMsg extends OverlayMessage implements VmInstanceMessage {
    private String vmInstanceUuid;
    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }
    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
