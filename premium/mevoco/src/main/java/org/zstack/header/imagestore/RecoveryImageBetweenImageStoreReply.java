package org.zstack.header.imagestore;

import org.zstack.header.message.MessageReply;

/**
 * Created by mingjian.deng on 2017/9/12.
 */
public class RecoveryImageBetweenImageStoreReply extends MessageReply {
    private String taskId;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
}
