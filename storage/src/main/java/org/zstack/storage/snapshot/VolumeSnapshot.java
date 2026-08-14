package org.zstack.storage.snapshot;

import org.zstack.header.message.Message;

/**
 */
public interface VolumeSnapshot {
    void handleMessage(Message msg);
}
