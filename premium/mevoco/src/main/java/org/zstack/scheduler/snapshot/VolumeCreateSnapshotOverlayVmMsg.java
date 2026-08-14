package org.zstack.scheduler.snapshot;

import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.message.OverlayMessage;
import org.zstack.header.vm.VmInstanceMessage;

import java.util.concurrent.TimeUnit;

@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 3)
public class VolumeCreateSnapshotOverlayVmMsg extends OverlayMessage implements VmInstanceMessage {
    private String vmInstanceUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
