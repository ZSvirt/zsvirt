package org.zstack.header.vm;

import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by lining on 2018/10/30.
 */
public class DetachIsoFromVmInstanceMsg extends NeedReplyMessage implements VmInstanceMessage{
    private String vmInstanceUuid;

    private String isoUuid;

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
}
