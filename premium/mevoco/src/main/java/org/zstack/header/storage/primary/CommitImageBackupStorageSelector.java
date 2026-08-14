package org.zstack.header.storage.primary;

import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.volume.VolumeInventory;

/**
 * Created by david on 7/30/16.
 */
public interface CommitImageBackupStorageSelector {
     BackupStorageInventory selectWithVolume(VolumeInventory vol, long requiredSize);
}
