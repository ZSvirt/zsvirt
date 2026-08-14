package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmInstanceSpec.IsoSpec;

/**
 * Created by frank on 10/17/2015.
 */
public class AttachIsoOnHypervisorMsg extends NeedReplyMessage implements HostMessage {
    private String vmInstanceUuid;
    private String hostUuid;
    private IsoSpec isoSpec;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public IsoSpec getIsoSpec() {
        return isoSpec;
    }

    public void setIsoSpec(IsoSpec isoSpec) {
        this.isoSpec = isoSpec;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
