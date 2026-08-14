package org.zstack.header.allocator;

import org.zstack.header.vm.VmInstanceMessage;

public class LastHostPreferredAllocateHostMsg extends AllocateHostMsg implements VmInstanceMessage {
    private String vmInstanceUuid;
    private String lastHostUuid;

    public String getLastHostUuid() {
        return lastHostUuid;
    }

    public void setLastHostUuid(String lastHostUuid) {
        this.lastHostUuid = lastHostUuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
