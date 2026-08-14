package org.zstack.imagereplicator;

import org.zstack.header.storage.backup.VolumeBackupInventory;

public interface VolumeBackupJournalGenerator {
    void generateInitialRecords(String bsUuid);
    void onUpdateVolumeBackup(VolumeBackupInventory inv);
    void onAddVolumeBackup(VolumeBackupInventory inv);
    void onExpungeVolumeBackup(String volumeBackupUuid, String bsUuid);
}
