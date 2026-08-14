package org.zstack.storage.migration.backup;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.image.ImageBackupStorageRefVO;
import org.zstack.header.image.ImageBackupStorageRefVO_;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;
import org.zstack.storage.backup.BackupStorageCapacityUpdater;
import org.zstack.storage.migration.StorageMigrationConstant;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

/**
 * Created by GuoYi on 10/4/17.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ReserveCapacityFromDstBSFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(ReserveCapacityFromDstBSFlow.class);

    @Autowired
    protected DatabaseFacade dbf;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        String imageUuid = (String) data.get(StorageMigrationConstant.IMAGE_UUID);
        long srcImageSize = Q.New(ImageVO.class)
                .select(ImageVO_.size)
                .eq(ImageVO_.uuid, imageUuid)
                .findValue();
        data.put(StorageMigrationConstant.IMAGE_SIZE, srcImageSize);

        String srcImageInstallPath = Q.New(ImageBackupStorageRefVO.class)
                .eq(ImageBackupStorageRefVO_.imageUuid, imageUuid)
                .select(ImageBackupStorageRefVO_.installPath)
                .findValue();
        data.put(StorageMigrationConstant.SRC_IMAGE_INSTALL_PATH, srcImageInstallPath);

        String dstBsUuid = (String) data.get(StorageMigrationConstant.DST_BS_UUID);
        BackupStorageCapacityUpdater capacityUpdater = new BackupStorageCapacityUpdater(dstBsUuid);
        capacityUpdater.reserveCapacity(srcImageSize, true);
        trigger.next();
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        long imageSize = (long) data.get(StorageMigrationConstant.IMAGE_SIZE);
        String dstBsUuid = (String) data.get(StorageMigrationConstant.DST_BS_UUID);
        BackupStorageCapacityUpdater capacityUpdater = new BackupStorageCapacityUpdater(dstBsUuid);
        capacityUpdater.increaseAvailableCapacity(imageSize);
        trigger.rollback();
    }
}
