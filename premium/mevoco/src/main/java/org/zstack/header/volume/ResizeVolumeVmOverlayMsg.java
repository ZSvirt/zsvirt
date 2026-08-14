package org.zstack.header.volume;

import org.zstack.header.message.OverlayMessage;
import org.zstack.header.storage.primary.PrimaryStorageMessage;
import org.zstack.header.vm.VmInstanceMessage;

/**
 * @author camile
 * @date 2017/11/10
 */
public class ResizeVolumeVmOverlayMsg extends OverlayMessage implements VmInstanceMessage {

    private String vmInstanceUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
