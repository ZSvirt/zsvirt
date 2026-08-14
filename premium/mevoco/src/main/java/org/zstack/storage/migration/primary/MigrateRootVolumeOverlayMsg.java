package org.zstack.storage.migration.primary;

import org.zstack.header.message.OverlayMessage;
import org.zstack.header.vm.VmInstanceMessage;

/**
 * Created by GuoYi on 9/19/17.
 */
public class MigrateRootVolumeOverlayMsg extends OverlayMessage implements VmInstanceMessage {
    private String vmInstanceUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
