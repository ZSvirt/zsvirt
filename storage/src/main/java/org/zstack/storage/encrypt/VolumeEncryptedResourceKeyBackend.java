package org.zstack.storage.encrypt;

import java.util.Set;

/**
 * Handles {@link org.zstack.header.volume.VolumeVO} rows in {@link org.zstack.header.keyprovider.EncryptedResourceKeyRefVO}
 * (key provider binding for LUKS volumes), analogous to {@link org.zstack.compute.vm.devices.TpmEncryptedResourceKeyBackend}
 * for TPM.
 */
public interface VolumeEncryptedResourceKeyBackend {

    /**
     * Link a volume to a key provider (placeholder ref row). Non-async.
     */
    void attachKeyProviderToVolume(String volumeUuid, String keyProviderUuid);

    /**
     * Remove key-provider binding for the volume. Non-async.
     */
    void detachKeyProviderFromVolume(String volumeUuid);

    /**
     * Remove key-provider binding for the snapshot. Non-async.
     */
    void detachKeyProviderFromSnapshot(String snapshotUuid);

    void detachKeyProviderFromTemporarySnapshotImage(String imageUuid);

    void detachKeyProviderFromBackup(String backupUuid);

    /**
     * @return provider uuid or null when not bound / crypto not installed
     */
    String findKeyProviderUuidByVolume(String volumeUuid);

    /**
     * Whether an {@code EncryptedResourceKeyRefVO} row exists for this volume.
     */
    boolean checkVolumeKeyProviderAttached(String volumeUuid);

    boolean checkSnapshotKeyProviderAttached(String snapshotUuid);

    boolean checkTemporarySnapshotImageKeyProviderAttached(String imageUuid);

    boolean hasBackupKeyRef(String backupUuid);

    void copyVolumeKeyRefToSnapshot(String volumeUuid, String snapshotUuid);

    void copyVolumeKeyRefToSnapshots(String volumeUuid, Set<String> snapshotUuids);

    void copySnapshotKeyRefToVolume(String snapshotUuid, String volumeUuid);

    void copyVolumeKeyRefToVolume(String srcVolumeUuid, String dstVolumeUuid);

    void copySnapshotKeyRefToTemporarySnapshotImage(String snapshotUuid, String imageUuid);

    void copyTemporarySnapshotImageKeyRefToVolume(String imageUuid, String volumeUuid);

    void copyVolumeKeyRefToBackup(String volumeUuid, String backupUuid);

    void copyBackupKeyRefToTemporarySnapshotImage(String backupUuid, String imageUuid);

    void copyBackupKeyRefToVolume(String backupUuid, String volumeUuid);

    /**
     * Global default key provider uuid, or null (e.g. NONE / crypto not installed).
     */
    String defaultKeyProviderUuid();

    String findKeyProviderUuidBySnapshot(String snapshotUuid);

    String findKeyProviderUuidByTemporarySnapshotImage(String imageUuid);

    /**
     * Current key version (DEK rotation generation) bound to this volume's
     * {@code EncryptedResourceKeyRefVO} row, or {@code null} when no row exists
     * (e.g. volume not yet bound to a key provider, or crypto not installed).
     *
     * <p>Mirrors {@link org.zstack.compute.vm.devices.TpmEncryptedResourceKeyBackend#findKeyVersionByTpm}.
     * Used by the start_vm path to derive the libvirt secret identity tuple
     * ({@code vmUuid, purpose="volume", keyVersion, usageInstance}) without
     * re-materializing the DEK.
     */
    Integer findKeyVersionByVolume(String volumeUuid);

    Integer findKeyVersionBySnapshot(String snapshotUuid);
}
