package org.zstack.imagereplicator;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.storage.database.backup.DatabaseBackupInventory;
import org.zstack.header.storage.database.backup.DatabaseBackupStorageRefInventory;
import org.zstack.header.storage.database.backup.DatabaseBackupStorageRefVO;
import org.zstack.header.storage.database.backup.DatabaseBackupStorageRefVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Set;
import java.util.stream.Collectors;

public class DatabaseBackupJournalGeneratorImpl implements DatabaseBackupJournalGenerator {
    private static final CLogger logger = Utils.getLogger(DatabaseBackupJournalGeneratorImpl.class);

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void generateInitialRecords(String bsUuid) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                if (q(ImageOpsJournalVO.class)
                        .eq(ImageOpsJournalVO_.backupStorageUuid, bsUuid)
                        .eq(ImageOpsJournalVO_.type, JournalType.DatabaseBackup)
                        .isExists()) {
                    return;
                }

                q(DatabaseBackupStorageRefVO.class)
                        .eq(DatabaseBackupStorageRefVO_.backupStorageUuid, bsUuid)
                        .select(DatabaseBackupStorageRefVO_.databaseBackupUuid)
                        .list()
                        .forEach(databaseBackupUuid -> persist(newOpsVO(bsUuid, databaseBackupUuid.toString(), ImageAction.Add)));
            }
        }.execute();
    }

    @Override
    public void onUpdateDatabaseBackup(DatabaseBackupInventory inv) {
        Set<String> bsUuids = inv.getBackupStorageRefs().stream()
                .map(DatabaseBackupStorageRefInventory::getBackupStorageUuid)
                .collect(Collectors.toSet());
        logger.info(String.format("DatabaseBackup[uuid:%s] is updated, BS[uuid:%s]",
                inv.getUuid(), String.join(",", bsUuids)));

        logger.info("generating journal for updating database backup: " + inv.getUuid());

        new SQLBatch() {
            @Override
            protected void scripts() {
                inv.getBackupStorageRefs().forEach(refInv -> {
                    persist(newOpsVO(refInv.getBackupStorageUuid(), refInv.getDatabaseBackupUuid(), ImageAction.Enable));
                });
            }
        }.execute();
    }

    @Override
    public void onAddDatabaseBackup(DatabaseBackupInventory inv) {
        Set<String> bsUuids = inv.getBackupStorageRefs().stream()
                .map(DatabaseBackupStorageRefInventory::getBackupStorageUuid)
                .collect(Collectors.toSet());
        logger.info(String.format("DatabaseBackup[uuid:%s] is added to BS[uuid:%s]",
                inv.getUuid(), String.join(",", bsUuids)));

        new SQLBatch() {
            @Override
            protected void scripts() {
                inv.getBackupStorageRefs().forEach(refInv -> {
                    persist(newOpsVO(refInv.getBackupStorageUuid(), refInv.getDatabaseBackupUuid(), ImageAction.Add));
                });
            }
        }.execute();
    }

    @Override
    public void onExpungeDatabaseBackup(String databaseBackupUuid, String bsUuid) {
        logger.info(String.format("DatabaseBackup[uuid:%s] is expunged from BS[uuid:%s]",
                databaseBackupUuid, bsUuid));

        ImageOpsJournalVO vo = newOpsVO(bsUuid, databaseBackupUuid, ImageAction.Expunge);
        dbf.persist(vo);
    }

    private static ImageOpsJournalVO newOpsVO(String bsUuid, String imageUuid, ImageAction action) {
        ImageOpsJournalVO vo = new ImageOpsJournalVO();
        vo.setBackupStorageUuid(bsUuid);
        vo.setImageUuid(imageUuid);
        vo.setAction(action);
        vo.setType(JournalType.DatabaseBackup);
        return vo;
    }
}
