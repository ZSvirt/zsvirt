package org.zstack.header.imagestore;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by mingjian.deng on 2017/9/13.
 */
public class GetSyncTaskStatusMsg extends NeedReplyMessage {
    private String taskId;
    private String bsUuid;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getBsUuid() {
        return bsUuid;
    }

    public void setBsUuid(String bsUuid) {
        this.bsUuid = bsUuid;
    }
}
