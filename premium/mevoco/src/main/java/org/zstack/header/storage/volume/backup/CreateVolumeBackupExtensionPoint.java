package org.zstack.header.storage.volume.backup;

import org.zstack.header.storage.backup.VolumeBackupVO;

import java.util.Collection;

/**
 * Created by MaJin on 2019/4/25.
 */
public interface CreateVolumeBackupExtensionPoint {
    void beforeCreateVolumeBackup(VolumeBackupVO vo, String bsUuid);

    void afterCreateVolumeBackup(VolumeBackupVO vo, String bsUuid);

    void failedToCreateVolumeBackup(VolumeBackupVO vo, String bsUuid);

    default void beforeCreateVmBackup(Collection<VolumeBackupVO> backups, String bsUuid) {
        backups.forEach((it) -> beforeCreateVolumeBackup(it, bsUuid));
    }

    default void afterCreateVmBackup(Collection<VolumeBackupVO> backups, String bsUuid) {
        backups.forEach((it) -> afterCreateVolumeBackup(it, bsUuid));
    }

    default void failedToCreateVmBackup(Collection<VolumeBackupVO> backups, String bsUuid) {
        backups.forEach((it) -> failedToCreateVolumeBackup(it, bsUuid));
    }
}
