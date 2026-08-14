package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

public class AttachIsoToVmInstanceMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String vmInstanceUuid;
    private String isoUuid;
    private String cdRomUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getIsoUuid() {
        return isoUuid;
    }

    public void setIsoUuid(String isoUuid) {
        this.isoUuid = isoUuid;
    }

    public String getCdRomUuid() {
        return cdRomUuid;
    }

    public void setCdRomUuid(String cdRomUuid) {
        this.cdRomUuid = cdRomUuid;
    }
}
