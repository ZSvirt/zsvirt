package org.zstack.storage.primary.local;

import org.zstack.header.message.OverlayMessage;
import org.zstack.header.vm.SkipVmTracer;
import org.zstack.header.vm.VmInstanceMessage;

/**
 * Created by MaJin on 2017-06-23.
 */
@SkipVmTracer(replyClass = MigrateVolumeOnLocalStorageReply.class)
public class MigrateRootVolumeVmOverlayMsg extends OverlayMessage implements VmInstanceMessage {
    private String vmInstanceUuid;


    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }
}
