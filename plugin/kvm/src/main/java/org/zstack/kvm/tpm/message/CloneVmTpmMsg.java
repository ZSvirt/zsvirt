package org.zstack.kvm.tpm.message;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class CloneVmTpmMsg extends NeedReplyMessage {
    private String srcVmUuid;
    private List<String> dstVmUuidList;
    private Boolean resetTpm;

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
}
