package org.zstack.imagereplicator;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.storage.backup.VolumeBackupInventory;
import org.zstack.header.storage.backup.VolumeBackupStorageRefInventory;
import org.zstack.header.storage.backup.VolumeBackupStorageRefVO;
import org.zstack.header.storage.backup.VolumeBackupStorageRefVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Set;
import java.util.stream.Collectors;

public class VolumeBackupJournalGeneratorImpl implements VolumeBackupJournalGenerator {
    private static final CLogger logger = Utils.getLogger(VolumeBackupJournalGeneratorImpl.class);

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void generateInitialRecords(String bsUuid) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                if (q(ImageOpsJournalVO.class)
                        .eq(ImageOpsJournalVO_.backupStorageUuid, bsUuid)
                        .eq(ImageOpsJournalVO_.type, JournalType.VolumeBackup)
                        .isExists()) {
                    return;
                }

                q(VolumeBackupStorageRefVO.class)
                        .eq(VolumeBackupStorageRefVO_.backupStorageUuid, bsUuid)
                        .select(VolumeBackupStorageRefVO_.volumeBackupUuid)
                        .list()
                        .forEach(volumeBackupUuid -> persist(newOpsVO(bsUuid, volumeBackupUuid.toString(), ImageAction.Add)));
            }
        }.execute();
    }

    @Override
    public void onUpdateVolumeBackup(VolumeBackupInventory inv) {
        Set<String> bsUuids = inv.getBackupStorageRefs().stream()
                .map(VolumeBackupStorageRefInventory::getBackupStorageUuid)
                .collect(Collectors.toSet());
        logger.info(String.format("VolumeBackup[uuid:%s] is updated, BS[uuid:%s]",
                inv.getUuid(), String.join(",", bsUuids)));

        logger.info("generating journal for updating volumebackup: " + inv.getUuid());

        new SQLBatch() {
            @Override
            protected void scripts() {
                inv.getBackupStorageRefs().forEach(refInv -> {
                    persist(newOpsVO(refInv.getBackupStorageUuid(), refInv.getVolumeBackupUuid(), ImageAction.Enable));
                });
            }
        }.execute();
    }

    @Override
    public void onAddVolumeBackup(VolumeBackupInventory inv) {
        Set<String> bsUuids = inv.getBackupStorageRefs().stream()
                .map(VolumeBackupStorageRefInventory::getBackupStorageUuid)
                .collect(Collectors.toSet());
        logger.info(String.format("VolumeBackup[uuid:%s] is added to BS[uuid:%s]",
                inv.getUuid(), String.join(",", bsUuids)));

        new SQLBatch() {
            @Override
            protected void scripts() {
                inv.getBackupStorageRefs().forEach(refInv -> {
                    persist(newOpsVO(refInv.getBackupStorageUuid(), refInv.getVolumeBackupUuid(), ImageAction.Add));
                });
            }
        }.execute();
    }

    @Override
    public void onExpungeVolumeBackup(String volumeBackupUuid, String bsUuid) {
        logger.info(String.format("VolumeBackup[uuid:%s] is expunged from BS[uuid:%s]",
                volumeBackupUuid, bsUuid));

        ImageOpsJournalVO vo = newOpsVO(bsUuid, volumeBackupUuid, ImageAction.Expunge);
        dbf.persist(vo);
    }

    private static ImageOpsJournalVO newOpsVO(String bsUuid, String imageUuid, ImageAction action) {
        ImageOpsJournalVO vo = new ImageOpsJournalVO();
        vo.setBackupStorageUuid(bsUuid);
        vo.setImageUuid(imageUuid);
        vo.setAction(action);
        vo.setType(JournalType.VolumeBackup);
        return vo;
    }
}
