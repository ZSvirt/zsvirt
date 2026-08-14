package org.zstack.storage.snapshot.group;

import org.zstack.header.message.Message;

/**
 * Created by MaJin on 2019/7/9.
 */
public interface VolumeSnapshotGroup {
    void handleMessage(Message msg);
}
