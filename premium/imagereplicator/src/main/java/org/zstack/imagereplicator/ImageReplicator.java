package org.zstack.imagereplicator;

import org.zstack.header.core.Completion;

public interface ImageReplicator {
    void start();
    void stop();
    void forceReplicate(String backupStorageUuid, Completion completion);
}
