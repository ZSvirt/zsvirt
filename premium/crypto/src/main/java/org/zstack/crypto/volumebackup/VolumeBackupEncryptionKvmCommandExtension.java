package org.zstack.crypto.volumebackup;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.storage.backup.VolumeBackupVO;
import org.zstack.header.storage.volume.backup.VmBackupKvmCommandExtensionContext;
import org.zstack.header.storage.volume.backup.VmBackupKvmCommandExtensionPoint;
import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.encrypt.VolumeEncryptedResourceKeyBackend;
import org.zstack.storage.encrypt.VolumeEncryptedSecretHelper;
import org.zstack.utils.CollectionUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.zstack.core.Platform.operr;

public class VolumeBackupEncryptionKvmCommandExtension implements VmBackupKvmCommandExtensionPoint {
    private static final String VOLUME_BACKUP_ENCRYPTION_ADDON = "VolumeBackupEncryption";
    private static final String COPIED_BACKUP_UUIDS = VolumeBackupEncryptionKvmCommandExtension.class.getName() + ".copiedBackupUuids";

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private VolumeEncryptedResourceKeyBackend volumeEncryptedResourceKeyBackend;
    @Autowired
    private VolumeEncryptedSecretHelper volumeEncryptedSecretHelper;

    @Override
    public void beforeTakeVmBackup(VmBackupKvmCommandExtensionContext context) {
        Set<String> copiedBackupUuids = new HashSet<>();
        context.getAttributes().put(COPIED_BACKUP_UUIDS, copiedBackupUuids);

        try {
            syncBackupRecords(context);

            List<BackupEncryptionSpec> specs = new ArrayList<>();
            for (VolumeVO volume : context.getVolumes()) {
                Integer deviceId = context.getDeviceIds().get(volume.getUuid());
                if (deviceId == null) {
                    throw operr("device id for volume[uuid:%s] not found", volume.getUuid()).toException();
                }

                BackupEncryptionSpec spec = makeBackupEncryptionSpec(volume, context.getHostUuid(), deviceId);
                specs.add(spec);

                if (spec.isEncrypted()) {
                    VolumeBackupVO backup = context.getVolumeBackups().get(volume.getUuid());
                    if (backup == null) {
                        throw operr("volume backup record for volume[uuid:%s] not found", volume.getUuid()).toException();
                    }

                    volumeEncryptedResourceKeyBackend.copyVolumeKeyToBackup(volume.getUuid(), backup.getUuid());
                    copiedBackupUuids.add(backup.getUuid());
                }
            }

            if (hasEncryptedBackup(specs)) {
                context.getAddons().put(VOLUME_BACKUP_ENCRYPTION_ADDON, new BackupEncryptionSpecAddon(specs));
                context.setFileTargetRequired(true);
            }
        } catch (RuntimeException e) {
            detachBackupKeyRefsByBackupUuids(copiedBackupUuids);
            throw e;
        }
    }

    @Override
    public void rollbackTakeVmBackup(VmBackupKvmCommandExtensionContext context) {
        Object copiedBackupUuids = context.getAttributes().get(COPIED_BACKUP_UUIDS);
        if (!(copiedBackupUuids instanceof Collection)) {
            return;
        }

        detachBackupKeyRefsByBackupUuids((Collection<?>) copiedBackupUuids);
    }

    private void syncBackupRecords(VmBackupKvmCommandExtensionContext context) {
        List<VolumeBackupVO> backups = new ArrayList<>();
        for (VolumeVO volume : context.getVolumes()) {
            VolumeBackupVO backup = context.getVolumeBackups().get(volume.getUuid());
            if (backup == null) {
                throw operr("volume backup record for volume[uuid:%s] not found", volume.getUuid()).toException();
            }

            backup.setEncrypted(volume.isEncrypted());
            backups.add(backup);
        }

        dbf.updateCollection(backups);
    }

    private BackupEncryptionSpec makeBackupEncryptionSpec(VolumeVO volume, String hostUuid, int deviceId) {
        BackupEncryptionSpec spec = new BackupEncryptionSpec();
        spec.setDeviceId(deviceId);
        spec.setEncrypted(volume.isEncrypted());
        spec.setCipher("luks");

        if (!volume.isEncrypted()) {
            return spec;
        }

        String encryptedDek = volumeEncryptedSecretHelper.prepareLuksEnvelopeDekOnHost(hostUuid, volume.getUuid());
        String keyProviderUuid = volumeEncryptedResourceKeyBackend.findKeyProviderUuidByVolume(volume.getUuid());
        Integer keyVersion = volumeEncryptedResourceKeyBackend.findKeyVersionByVolume(volume.getUuid());
        if (StringUtils.isBlank(encryptedDek) || StringUtils.isBlank(keyProviderUuid) || keyVersion == null) {
            throw operr("failed to prepare encrypted backup key material for volume[uuid:%s]", volume.getUuid()).toException();
        }

        spec.setEncryptedDek(encryptedDek);
        spec.setKeyProviderUuid(keyProviderUuid);
        spec.setKeyVersion(keyVersion);
        return spec;
    }

    private boolean hasEncryptedBackup(Collection<BackupEncryptionSpec> specs) {
        return specs != null && specs.stream().anyMatch(spec -> spec != null && spec.isEncrypted());
    }

    private void detachBackupKeyRefsByBackupUuids(Collection<?> backupUuids) {
        if (CollectionUtils.isEmpty(backupUuids)) {
            return;
        }

        backupUuids.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(StringUtils::isNotBlank)
                .forEach(volumeEncryptedResourceKeyBackend::detachKeyProviderFromBackup);
    }

    public static class BackupEncryptionSpec implements Serializable {
        private int deviceId;
        private boolean encrypted;
        private String cipher;
        private String encryptedDek;
        private String keyProviderUuid;
        private Integer keyVersion;

        public int getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(int deviceId) {
            this.deviceId = deviceId;
        }

        public boolean isEncrypted() {
            return encrypted;
        }

        public void setEncrypted(boolean encrypted) {
            this.encrypted = encrypted;
        }

        public String getCipher() {
            return cipher;
        }

        public void setCipher(String cipher) {
            this.cipher = cipher;
        }

        public String getEncryptedDek() {
            return encryptedDek;
        }

        public void setEncryptedDek(String encryptedDek) {
            this.encryptedDek = encryptedDek;
        }

        public String getKeyProviderUuid() {
            return keyProviderUuid;
        }

        public void setKeyProviderUuid(String keyProviderUuid) {
            this.keyProviderUuid = keyProviderUuid;
        }

        public Integer getKeyVersion() {
            return keyVersion;
        }

        public void setKeyVersion(Integer keyVersion) {
            this.keyVersion = keyVersion;
        }
    }

    public static class BackupEncryptionSpecAddon implements Serializable {
        private List<BackupEncryptionSpec> specs;

        public BackupEncryptionSpecAddon() {
        }

        public BackupEncryptionSpecAddon(List<BackupEncryptionSpec> specs) {
            this.specs = specs;
        }

        public List<BackupEncryptionSpec> getSpecs() {
            return specs;
        }

        public void setSpecs(List<BackupEncryptionSpec> specs) {
            this.specs = specs;
        }
    }
}
