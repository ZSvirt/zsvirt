package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by shixin on 09/15/2018.
 */
public class DeleteL3NetworkFromVmNicMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String vmInstanceUuid;
    private String vmNicUuid;
    private String newL3Uuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }

    public String getNewL3Uuid() {
        return newL3Uuid;
    }

    public void setNewL3Uuid(String newL3Uuid) {
        this.newL3Uuid = newL3Uuid;
    }
}
