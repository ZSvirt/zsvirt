package org.zstack.header.vm.cdrom;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmInstanceMessage;

/**
 * Created by lining on 2018/12/28.
 */
public class DeleteVmCdRomMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String cdRomUuid;

    private String vmInstanceUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public String getCdRomUuid() {
        return cdRomUuid;
    }

    public void setCdRomUuid(String cdRomUuid) {
        this.cdRomUuid = cdRomUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
