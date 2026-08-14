package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by GuoYi on 11/2/17.
 */
public class GetImageCandidatesForVmToChangeMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String vmInstanceUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
