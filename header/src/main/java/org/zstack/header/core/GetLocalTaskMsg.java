package org.zstack.header.core;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class GetLocalTaskMsg extends NeedReplyMessage {
    private List<String> syncSignatures;
    private boolean runningTasksOnly;

    public void setSyncSignatures(List<String> syncSignatures) {
        this.syncSignatures = syncSignatures;
    }

    public List<String> getSyncSignatures() {
        return syncSignatures;
    }

    public boolean isRunningTasksOnly() {
        return runningTasksOnly;
    }

    public void setRunningTasksOnly(boolean runningTasksOnly) {
        this.runningTasksOnly = runningTasksOnly;
    }
}