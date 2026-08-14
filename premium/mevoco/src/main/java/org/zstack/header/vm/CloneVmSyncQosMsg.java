package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by mingjian.deng on 17/1/25.
 */
public class CloneVmSyncQosMsg extends NeedReplyMessage implements VmInstanceMessage {
    private VmInstanceInventory srcVm;
    private String dstVmUuid;
    private boolean full = false;

    public boolean isFull() {
        return full;
    }

    public void setFull(boolean full) {
        this.full = full;
    }

    public VmInstanceInventory getSrcVm() {
        return srcVm;
    }

    public void setSrcVm(VmInstanceInventory srcVm) {
        this.srcVm = srcVm;
    }

    @Override
    public String getVmInstanceUuid() {
        return dstVmUuid;
    }

    public String getDstVmUuid() {
        return dstVmUuid;
    }

    public void setDstVmUuid(String dstVmUuid) {
        this.dstVmUuid = dstVmUuid;
    }
}
