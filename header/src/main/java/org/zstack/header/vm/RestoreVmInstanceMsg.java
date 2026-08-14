package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

@SkipVmTracer(replyClass = StartVmInstanceReply.class)
public class RestoreVmInstanceMsg extends NeedReplyMessage implements VmInstanceMessage, CheckAttachedVolumesMessage {
    private String vmInstanceUuid;
    private String memorySnapshotUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getMemorySnapshotUuid() {
        return memorySnapshotUuid;
    }

    public void setMemorySnapshotUuid(String memorySnapshotUuid) {
        this.memorySnapshotUuid = memorySnapshotUuid;
    }
}

