package org.zstack.storage.migration.backup.ceph;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.trash.StorageTrash;
import org.zstack.core.trash.TrashType;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.storage.migration.StorageMigrationConstant;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

/**
 * Created by GuoYi on 10/4/17.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CephDeleteImageFileFromSrcBSFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(CephDeleteImageFileFromSrcBSFlow.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    private StorageTrash trash;

    private static final String TRASH_ID = "trash_id";

    @Override
    public String name() {
        return "ceph-delete-image-file-from-src-bs";
    }

    @Override
    public void run(FlowTrigger trigger, Map data) {
        String srcBsUuid = (String) data.get(StorageMigrationConstant.SRC_BS_UUID);
        String srcImageInstallPath = (String) data.get(StorageMigrationConstant.SRC_IMAGE_INSTALL_PATH);
        long imageSize = (long) data.get(StorageMigrationConstant.IMAGE_SIZE);
        String imageUuid = (String) data.get(StorageMigrationConstant.IMAGE_UUID);

        ImageInventory image = ImageInventory.valueOf(dbf.findByUuid(imageUuid, ImageVO.class));
        image.setSize(imageSize);
        image.setUrl(srcImageInstallPath);
        //the description field temporarily records the uuid value of image storage
        image.setDescription(srcBsUuid);
        data.put(TRASH_ID, trash.createTrash(TrashType.MigrateImage, false, image).getTrashId());

        trigger.next();
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        if (data.get(TRASH_ID) != null) {
            trash.removeFromDb((Long)data.get(TRASH_ID));
        }
        trigger.rollback();
    }
}
