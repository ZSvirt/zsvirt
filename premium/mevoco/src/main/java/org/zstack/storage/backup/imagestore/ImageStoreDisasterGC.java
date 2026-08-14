package org.zstack.storage.backup.imagestore;

import org.zstack.core.db.Q;
import org.zstack.core.gc.CycleBasedGarbageCollector;
import org.zstack.core.gc.GCCompletion;
import org.zstack.header.image.ImageVO;
import org.zstack.header.imagestore.SyncTaskStatus;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

/**
 * Created by xing5 on 2017/3/6.
 */
public class ImageStoreDisasterGC extends CycleBasedGarbageCollector {
    private static final CLogger logger = Utils.getLogger(ImageStoreDisasterGC.class);

    public static String getGCName() {
        return "gc-start-image-store-disaster";
    }

    private void gcTags(final SystemTagInventory tag) {
        String[] tokens = tag.getTag().split("::");

        DebugUtils.Assert(tokens.length == 8, String.format("system tag ill formated: %s", tag.getTag()));
        if (tokens[7].equals(SyncTaskStatus.TsSuccess.toString())) {
            dbf.removeByPrimaryKey(tag.getUuid(), SystemTagVO.class);
        } else if (!dbf.isExist(tokens[1], ImageVO.class)) {
            logger.info(String.format("src image[%s] has been deleted, gc system tag.", tokens[1]));
            dbf.removeByPrimaryKey(tag.getUuid(), SystemTagVO.class);
        } else if (!dbf.isExist(tokens[5], ImageVO.class)) {
            logger.info(String.format("dst image[%s] has been deleted, gc system tag.", tokens[5]));
            dbf.removeByPrimaryKey(tag.getUuid(), SystemTagVO.class);
        }

    }

    private void findAndGcTags(final SystemTagInventory tag) {
        if (System.currentTimeMillis() - tag.getLastOpDate().getTime() > ImageStoreBackupStorageConstant.RESPONSE_TASK_RESERVE_TIME) {
            gcTags(tag);
        }
    }

    @Override
    protected void triggerNow(GCCompletion completion) {
        logger.debug("trigger: " + getGCName());
        List<ImageStoreBackupStorageVO> bss = Q.New(ImageStoreBackupStorageVO.class).list();

        for(ImageStoreBackupStorageVO bs: bss) {
            List<SystemTagInventory> tags = ImageStoreSystemTags.SYNC_TASK_STATUS.getTagInventories(bs.getUuid());
            tags.forEach(tag -> findAndGcTags(tag));
        }

        // delete old version
        for(ImageStoreBackupStorageVO bs: bss) {
            List<SystemTagInventory> tags = ImageStoreSystemTags.SYNC_TASK_STATUS_OLD.getTagInventories(bs.getUuid());
            tags.forEach(tag -> dbf.removeByPrimaryKey(tag.getUuid(), SystemTagVO.class));
        }

        completion.success();
    }
}
