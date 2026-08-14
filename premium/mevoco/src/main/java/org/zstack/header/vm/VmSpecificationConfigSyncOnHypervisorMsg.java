package org.zstack.header.vm;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

public class VmSpecificationConfigSyncOnHypervisorMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    private String vmInstanceUuid;
    private VmConfigSyncStruct.VmSpecificationConfig spec;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public VmConfigSyncStruct.VmSpecificationConfig getSpec() {
        return spec;
    }

    public void setSpec(VmConfigSyncStruct.VmSpecificationConfig spec) {
        this.spec = spec;
    }
}
