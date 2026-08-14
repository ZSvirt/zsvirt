package org.zstack.header.sriov;

import org.zstack.header.message.NeedReplyMessage;

public class ChangeVfNicHaStateMsg extends NeedReplyMessage {
    private String vfNicUuid;
    private String haState;
    private Boolean isForceUpdate=false;

    public String getVfNicUuid() {
        return vfNicUuid;
    }

    public void setVfNicUuid(String vmNicUuid) {
        this.vfNicUuid = vmNicUuid;
    }

    public String getHaState() {
        return haState;
    }

    public void setHaState(String haState) {
        this.haState = haState;
    }

    public Boolean getForceUpdate() {
        return isForceUpdate;
    }

    public void setForceUpdate(Boolean isForceUpdate ) {
        this.isForceUpdate = isForceUpdate;
    }
}
