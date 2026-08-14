package org.zstack.header.imagestore;

import org.zstack.header.message.MessageReply;

public class PushBitsBetweenImageStoreReply extends MessageReply {
    private String taskId;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
}
