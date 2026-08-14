package org.zstack.header.volumebackup;

import java.util.Map;

/**
 * Extension point for collecting and restoring extra metadata attachments for volume backups.
 *
 * Note: implementations are expected to be in premium modules that have access to VolumeBackup VO types.
 */
public interface VolumeBackupMetadataExtensionPoint {
    /**
     * Collect additional metadata for a volume backup and put JSON-serialized POJOs into attachments map.
     * @param volumeBackupUuid the volume backup uuid
     * @param attachments the attachments map to fill (key -> JSON string)
     */
    void collectMetadata(String volumeBackupUuid, Map<String, String> attachments);

    /**
     * Restore additional metadata from attachments to target backup/restore flow.
     * @param volumeBackupUuid the volume backup uuid
     * @param attachments the attachments map read from backup metadata
     */
    void restoreMetadata(String volumeBackupUuid, Map<String, String> attachments);
}
