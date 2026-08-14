package org.zstack.core.debug;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by LiangHanYu on 2021/5/29 21:26
 */
public class CleanQueueMsg extends NeedReplyMessage {
    private String signatureName;
    private Integer taskIndex;
    private Boolean isCleanUp = false;
    private Boolean isRunningTask = true;

    public String getSignatureName() {
        return signatureName;
    }

    public void setSignatureName(String signatureName) {
        this.signatureName = signatureName;
    }

    public Integer getTaskIndex() {
        return taskIndex;
    }

    public void setTaskIndex(Integer taskIndex) {
        this.taskIndex = taskIndex;
    }

    public Boolean getCleanUp() {
        return isCleanUp;
    }

    public void setCleanUp(Boolean cleanUp) {
        isCleanUp = cleanUp;
    }

    public Boolean getRunningTask() {
        return isRunningTask;
    }

    public void setRunningTask(Boolean runningTask) {
        isRunningTask = runningTask;
    }
}
