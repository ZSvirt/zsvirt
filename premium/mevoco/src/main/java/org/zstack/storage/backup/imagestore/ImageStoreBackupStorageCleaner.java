package org.zstack.storage.backup.imagestore;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.image.ExpungeImageExtensionPoint;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.BackupStorageVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class ImageStoreBackupStorageCleaner implements ExpungeImageExtensionPoint {
    private static final CLogger logger = Utils.getLogger(ImageStoreBackupStorageCleaner.class);

    @Autowired
    private CloudBus bus;

    @Override
    public void preExpungeImage(ImageInventory img) {
    }

    @Override
    public void beforeExpungeImage(ImageInventory img) {
    }

    private String getBackupStorageType(String backupStorageUuid) {
        return Q.New(BackupStorageVO.class)
                .eq(BackupStorageVO_.uuid, backupStorageUuid)
                .select(BackupStorageVO_.type)
                .findValue();
    }

    @Override
    public void afterExpungeImage(ImageInventory img, String backupStorageUuid) {
        if (!ImageStoreGlobalConfig.CLEAN_IMAGESTORE_ON_EXPUNGE.value(Boolean.class)) {
            return;
        }

        if (!getBackupStorageType(backupStorageUuid).equals(ImageStoreBackupStorageConstant.IMAGE_STORE_BACKUP_STORAGE_TYPE)) {
            return;
        }

        logger.info("cleaning imagestore, uuid=" + backupStorageUuid);

        ReclaimSpaceFromImageStoreMsg rmsg = new ReclaimSpaceFromImageStoreMsg();
        rmsg.setUuid(backupStorageUuid);
        bus.makeTargetServiceIdByResourceUuid(rmsg, BackupStorageConstant.SERVICE_ID, rmsg.getUuid());
        bus.send(rmsg);
    }

    @Override
    public void failedToExpungeImage(ImageInventory img, ErrorCode err) {
    }
}
