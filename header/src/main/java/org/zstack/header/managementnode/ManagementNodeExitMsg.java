package org.zstack.header.managementnode;

import org.zstack.header.message.Message;

public class ManagementNodeExitMsg extends Message {
    public static enum Reason {
        Normal,
        HeartBeatStopped
    }

    private Reason reason = Reason.Normal;
    private String details;

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Reason getReason() {
        return reason;
    }

    public void setReason(Reason reason) {
        this.reason = reason;
    }

    public ManagementNodeExitMsg() {
    }
}
