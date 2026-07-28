package org.zstack.crypto.volumebackup;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.storage.backup.VolumeBackupStorageRefVO;
import org.zstack.header.storage.backup.VolumeBackupStorageRefVO_;
import org.zstack.header.storage.backup.VolumeBackupVO;
import org.zstack.header.storage.backup.VolumeBackupVO_;
import org.zstack.header.volume.VolumeBackupEncryptionConversionCommitter;
import org.zstack.header.volume.VolumeBackupEncryptionConversionResult;
import org.zstack.storage.encrypt.VolumeEncryptedResourceKeyBackend;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.zstack.core.Platform.operr;

public class VolumeBackupEncryptionConversionCommitterImpl implements VolumeBackupEncryptionConversionCommitter {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private VolumeEncryptedResourceKeyBackend volumeEncryptedResourceKeyBackend;

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void commitVolumeBackupEncryptionConversion(VolumeBackupEncryptionConversionResult result) {
        if (result == null) {
            return;
        }

        for (VolumeBackupEncryptionConversionResult.RefRepoint ref : result.getRefsToRepoint()) {
            if (StringUtils.isBlank(ref.getNewInstallPath())) {
                throw operr("missing converted installPath for backup ref[id:%s, backupStorageUuid:%s, oldInstallPath:%s]",
                        ref.getRefId(), ref.getBackupStorageUuid(), ref.getOldInstallPath()).toException();
            }
            VolumeBackupStorageRefVO current = Q.New(VolumeBackupStorageRefVO.class)
                    .eq(VolumeBackupStorageRefVO_.id, ref.getRefId())
                    .eq(VolumeBackupStorageRefVO_.volumeBackupUuid, ref.getBackupUuid())
                    .eq(VolumeBackupStorageRefVO_.backupStorageUuid, ref.getBackupStorageUuid())
                    .eq(VolumeBackupStorageRefVO_.installPath, ref.getOldInstallPath())
                    .find();
            if (current == null) {
                throw operr("backup ref changed during encryption conversion[id:%s, backupStorageUuid:%s, oldInstallPath:%s]",
                        ref.getRefId(), ref.getBackupStorageUuid(), ref.getOldInstallPath()).toException();
            }
            current.setInstallPath(ref.getNewInstallPath());
        }

        Set<String> backupUuids = new LinkedHashSet<>(result.getConvertedBackupUuids());
        if (!backupUuids.isEmpty()) {
            SQL.New(VolumeBackupVO.class)
                    .in(VolumeBackupVO_.uuid, backupUuids)
                    .set(VolumeBackupVO_.encrypted, result.isTargetEncrypted())
                    .update();
        }
        backupUuids.forEach(backupUuid -> {
            if (result.isTargetEncrypted()) {
                volumeEncryptedResourceKeyBackend.copyVolumeKeyRefToBackup(result.getVolumeUuid(), backupUuid);
            } else {
                volumeEncryptedResourceKeyBackend.detachKeyProviderFromBackup(backupUuid);
            }
        });

        for (String placeholderBackupUuid : result.getPlaceholderBackupUuids()) {
            if (StringUtils.isNotBlank(placeholderBackupUuid)) {
                dbf.removeByPrimaryKey(placeholderBackupUuid, VolumeBackupVO.class);
            }
        }
    }
}
