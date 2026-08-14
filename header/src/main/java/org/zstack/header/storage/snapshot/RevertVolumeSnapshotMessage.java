package org.zstack.header.storage.snapshot;

import org.zstack.header.identity.SessionInventory;

/**
 * Created by MaJin on 2019/7/10.
 */
public interface RevertVolumeSnapshotMessage extends VolumeSnapshotMessage {
    SessionInventory getSession();
}
