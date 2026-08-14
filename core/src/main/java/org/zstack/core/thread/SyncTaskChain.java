package org.zstack.core.thread;

import org.zstack.header.core.AsyncBackup;

public interface SyncTaskChain extends AsyncBackup {
    void next();
}
