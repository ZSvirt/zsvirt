package org.zstack.header.vm;

import org.zstack.header.message.LockResourceMessage;

/**
 */
public class LockVmInstanceMsg extends LockResourceMessage implements VmInstanceMessage {
    private String vmInstanceUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
