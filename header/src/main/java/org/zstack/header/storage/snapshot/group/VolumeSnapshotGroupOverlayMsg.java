package org.zstack.header.storage.snapshot.group;

import org.zstack.header.message.OverlayMessage;
import org.zstack.header.vm.VmInstanceMessage;

/**
 * Created by MaJin on 2019/7/10.
 */
public class VolumeSnapshotGroupOverlayMsg extends OverlayMessage implements VmInstanceMessage {
    private String vmInstanceUuid;

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }
}
