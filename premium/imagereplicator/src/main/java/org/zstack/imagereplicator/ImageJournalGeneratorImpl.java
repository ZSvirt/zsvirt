package org.zstack.imagereplicator;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.image.ImageBackupStorageRefInventory;
import org.zstack.header.image.ImageBackupStorageRefVO;
import org.zstack.header.image.ImageBackupStorageRefVO_;
import org.zstack.header.image.ImageInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Set;
import java.util.stream.Collectors;

public class ImageJournalGeneratorImpl implements ImageJournalGenerator {
    private static final CLogger logger = Utils.getLogger(ImageJournalGeneratorImpl.class);

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void generateInitialRecords(String bsUuid) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                if (q(ImageOpsJournalVO.class)
                        .eq(ImageOpsJournalVO_.backupStorageUuid, bsUuid)
                        .eq(ImageOpsJournalVO_.type, JournalType.Image)
                        .isExists()) {
                    return;
                }

                q(ImageBackupStorageRefVO.class)
                        .eq(ImageBackupStorageRefVO_.backupStorageUuid, bsUuid)
                        .select(ImageBackupStorageRefVO_.imageUuid)
                        .listValues()
                        .forEach(imageUuid -> persist(newOpsVO(bsUuid, imageUuid.toString(), ImageAction.Add)));
            }
        }.execute();
    }

    @Override
    public void onUpdateImage(ImageInventory inv) {
        Set<String> bsUuids = inv.getBackupStorageRefs().stream()
                .map(ImageBackupStorageRefInventory::getBackupStorageUuid)
                .collect(Collectors.toSet());
        logger.info(String.format("Image[uuid:%s] is updated, BS[uuid:%s]",
                inv.getUuid(), String.join(",", bsUuids)));

        logger.info("generating journal for updating image: " + inv.getUuid());

        new SQLBatch() {
            @Override
            protected void scripts() {
                inv.getBackupStorageRefs().forEach(refInv -> {
                    persist(newOpsVO(refInv.getBackupStorageUuid(), refInv.getImageUuid(), ImageAction.Enable));
                });
            }
        }.execute();
    }

    @Override
    public void onAddImage(ImageInventory inv) {
        Set<String> bsUuids = inv.getBackupStorageRefs().stream()
                .map(ImageBackupStorageRefInventory::getBackupStorageUuid)
                .collect(Collectors.toSet());
        logger.info(String.format("Image[uuid:%s] is added to BS[uuid:%s]",
                inv.getUuid(), String.join(",", bsUuids)));

        inv.getBackupStorageRefs().forEach(refInv -> {
            ImageOpsJournalVO journalVO = newOpsVO(refInv.getBackupStorageUuid(), refInv.getImageUuid(), ImageAction.Add);

            logger.debug(String.format("persist add image[uuid: %s] to bs[uuid: %s] journal to db", refInv.getImageUuid(), refInv.getBackupStorageUuid()));
            dbf.persist(journalVO);
        });
    }

    @Override
    public void onExpungeImage(String imageUuid, String bsUuid) {
        logger.info(String.format("Image[uuid:%s] is expunged from BS[uuid:%s]",
                imageUuid, bsUuid));

        ImageOpsJournalVO vo = newOpsVO(bsUuid, imageUuid, ImageAction.Expunge);
        dbf.persist(vo);
    }

    private static ImageOpsJournalVO newOpsVO(String bsUuid, String imageUuid, ImageAction action) {
        ImageOpsJournalVO vo = new ImageOpsJournalVO();
        vo.setBackupStorageUuid(bsUuid);
        vo.setImageUuid(imageUuid);
        vo.setAction(action);
        vo.setType(JournalType.Image);
        return vo;
    }
}
