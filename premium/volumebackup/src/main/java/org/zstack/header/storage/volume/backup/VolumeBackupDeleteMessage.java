package org.zstack.header.storage.volume.backup;

/**
 * Created by kayo on 2018/7/25.
 */
public interface VolumeBackupDeleteMessage {
    String getVolumeBackupUuid();

    boolean isHandleDependency();
}
