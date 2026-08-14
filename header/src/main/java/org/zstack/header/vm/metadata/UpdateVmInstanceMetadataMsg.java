package org.zstack.header.vm.metadata;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmInstanceMessage;

public class UpdateVmInstanceMetadataMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String vmInstanceUuid;
    private boolean storageStructureChange;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public boolean isStorageStructureChange() {
        return storageStructureChange;
    }

    public void setStorageStructureChange(boolean storageStructureChange) {
        this.storageStructureChange = storageStructureChange;
    }
}
