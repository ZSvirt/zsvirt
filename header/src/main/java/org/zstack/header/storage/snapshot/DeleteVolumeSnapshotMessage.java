package org.zstack.header.storage.snapshot;

import org.zstack.header.message.APIDeleteMessage.DeletionMode;

/**
 * Created by MaJin on 2019/7/10.
 */
public interface DeleteVolumeSnapshotMessage extends VolumeSnapshotMessage {
    DeletionMode getDeletionMode();

    String getDirection();

    String getScope();
}
