package org.zstack.kvm.vmfiles.message;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class CloneVmHostFileMsg extends NeedReplyMessage {
    private String srcVmUuid;
    private List<String> dstVmUuidList;
    private Boolean resetTpm;
    private boolean ignoreSyncError = false;

    public String getSrcVmUuid() {
        return srcVmUuid;
    }

    public void setSrcVmUuid(String srcVmUuid) {
        this.srcVmUuid = srcVmUuid;
    }

    public List<String> getDstVmUuidList() {
        return dstVmUuidList;
    }

    public void setDstVmUuidList(List<String> dstVmUuidList) {
        this.dstVmUuidList = dstVmUuidList;
    }

    public Boolean getResetTpm() {
        return resetTpm;
    }

    public void setResetTpm(Boolean resetTpm) {
        this.resetTpm = resetTpm;
    }

    public boolean isIgnoreSyncError() {
        return ignoreSyncError;
    }

    public void setIgnoreSyncError(boolean ignoreSyncError) {
        this.ignoreSyncError = ignoreSyncError;
    }
}
