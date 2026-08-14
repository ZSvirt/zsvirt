package org.zstack.header.volume;

import org.zstack.header.message.MessageReply;

public class BatchSyncActiveVolumeSizeOnHostReply extends MessageReply {
    private Integer successCount = 0;

    private Integer failCount = 0;

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setFailCount(Integer failCount) {
        this.failCount = failCount;
    }

    public Integer getFailCount() {
        return failCount;
    }

    public synchronized void addSuccessCount(Integer successCount) {
        this.successCount = this.successCount + successCount;
    }

    public synchronized void addFailCount(Integer failCount) {
        this.failCount = this.failCount + failCount;
    }
}
