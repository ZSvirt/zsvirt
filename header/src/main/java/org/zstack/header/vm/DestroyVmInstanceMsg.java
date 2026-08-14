package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;

public class DestroyVmInstanceMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String vmInstanceUuid;

    private VmInstanceDeletionPolicy deletionPolicy;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public VmInstanceDeletionPolicy getDeletionPolicy() {
        return deletionPolicy;
    }

    public void setDeletionPolicy(VmInstanceDeletionPolicy deletionPolicy) {
        this.deletionPolicy = deletionPolicy;
    }
}
