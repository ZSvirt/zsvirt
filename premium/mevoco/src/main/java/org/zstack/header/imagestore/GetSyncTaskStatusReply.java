package org.zstack.header.imagestore;

import org.zstack.header.message.MessageReply;

/**
 * Created by mingjian.deng on 2017/9/13.
 */
public class GetSyncTaskStatusReply extends MessageReply {
    private SyncTaskStatus status;

    public SyncTaskStatus getStatus() {
        return status;
    }

    public void setStatus(SyncTaskStatus status) {
        this.status = status;
    }
}
