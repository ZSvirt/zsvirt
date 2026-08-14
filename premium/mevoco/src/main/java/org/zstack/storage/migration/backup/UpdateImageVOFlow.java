package org.zstack.storage.migration.backup;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.image.ImageBackupStorageRefVO;
import org.zstack.header.image.ImageBackupStorageRefVO_;
import org.zstack.header.image.ImageStatus;
import org.zstack.header.image.ImageVO;
import org.zstack.storage.migration.StorageMigrationConstant;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

/**
 * Created by GuoYi on 10/2/17.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class UpdateImageVOFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(UpdateImageVOFlow.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;

    @Override
    public String name() {
        return "update-image-vo";
    }

    @Override
    public void run(FlowTrigger trigger, Map data) {
        String dstImageInstallPath = (String) data.get(StorageMigrationConstant.DST_IMAGE_INSTALL_PATH);
        String imageUuid = (String) data.get(StorageMigrationConstant.IMAGE_UUID);
        String srcBsUuid = (String) data.get(StorageMigrationConstant.SRC_BS_UUID);
        String dstBsUuid = (String) data.get(StorageMigrationConstant.DST_BS_UUID);

        ImageVO image = dbf.findByUuid(imageUuid, ImageVO.class);
        image.setStatus(ImageStatus.Ready);
        dbf.update(image);

        ImageBackupStorageRefVO ref = Q.New(ImageBackupStorageRefVO.class)
                .eq(ImageBackupStorageRefVO_.backupStorageUuid, srcBsUuid)
                .eq(ImageBackupStorageRefVO_.imageUuid, imageUuid)
                .find();
        ref.setBackupStorageUuid(dstBsUuid);
        ref.setInstallPath(dstImageInstallPath);
        ref.setStatus(ImageStatus.Ready);
        dbf.update(ref);
        logger.info(String.format("Update ImageVO[uuid:%s] after image migration.", imageUuid));

        trigger.next();
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        String srcImageInstallPath = (String) data.get(StorageMigrationConstant.SRC_IMAGE_INSTALL_PATH);
        String imageUuid = (String) data.get(StorageMigrationConstant.IMAGE_UUID);
        String srcBsUuid = (String) data.get(StorageMigrationConstant.SRC_BS_UUID);
        String dstBsUuid = (String) data.get(StorageMigrationConstant.DST_BS_UUID);

        ImageBackupStorageRefVO ref = Q.New(ImageBackupStorageRefVO.class)
                .eq(ImageBackupStorageRefVO_.backupStorageUuid, dstBsUuid)
                .eq(ImageBackupStorageRefVO_.imageUuid, imageUuid)
                .find();
        ref.setBackupStorageUuid(srcBsUuid);
        ref.setInstallPath(srcImageInstallPath);
        dbf.update(ref);

        trigger.rollback();
    }
}
