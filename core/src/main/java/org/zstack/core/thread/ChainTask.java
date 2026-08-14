package org.zstack.core.thread;

import org.zstack.header.core.AsyncBackup;

public abstract class ChainTask extends AbstractChainTask {
    public ChainTask(AsyncBackup one, AsyncBackup...others) {
        super(one, others);
    }

    public abstract void run(SyncTaskChain chain);

    protected int getSyncLevel() {
        return 1;
    }

    /**
     * We add getDeduplicateString / getMaxPendingTasks / exceedMaxPendingCallback to support chain task qos
     * 1. getDeduplicateString is set for deduplicate the task in the same syncsignature queue
     * 2. getMaxPendingTasks is set for the pending queue length, start from 0, -1 means no limit
     * 3. exceedMaxPendingCallback is set for the callback if the task is canceled by exceed max pending queue
     */
    protected int getMaxPendingTasks() {
        return -1;
    }
    protected void exceedMaxPendingCallback() {
    }
    protected String getDeduplicateString() {
        return null;
    }
}
