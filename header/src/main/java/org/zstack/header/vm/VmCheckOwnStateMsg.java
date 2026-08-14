package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by frank on 11/9/2015.
 */
public class VmCheckOwnStateMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String vmInstanceUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
