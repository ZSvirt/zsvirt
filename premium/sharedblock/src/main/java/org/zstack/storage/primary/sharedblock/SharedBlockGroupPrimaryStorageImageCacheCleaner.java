package org.zstack.storage.primary.sharedblock;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.Q;
import org.zstack.core.gc.GCStatus;
import org.zstack.core.gc.GarbageCollector;
import org.zstack.core.gc.GarbageCollectorVO;
import org.zstack.core.gc.GarbageCollectorVO_;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.storage.primary.ImageCacheCleaner;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.List;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SharedBlockGroupPrimaryStorageImageCacheCleaner extends ImageCacheCleaner implements ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(SharedBlockGroupPrimaryStorageImageCacheCleaner.class);

    @Override
    public void managementNodeReady() {
        checkFalseGcJob();
        startGC();
    }

    @Override
    protected String getPrimaryStorageType() {
        return SharedBlockConstants.SHARED_BLOCK_PRIMARY_STORAGE_TYPE;
    }

    @ExceptionSafe
    public void checkFalseGcJob() {
        List<GarbageCollectorVO> gcs = Q.New(GarbageCollectorVO.class)
                .notEq(GarbageCollectorVO_.status, GCStatus.Done)
                .eq(GarbageCollectorVO_.runnerClass, SharedBlockDeleteVolumeGC.class.getName())
                .list();

        if (gcs == null || gcs.isEmpty()) {
            return;
        }

        for (GarbageCollectorVO gc : gcs) {
            String installPathByName = gc.getName().substring(gc.getName().lastIndexOf('-') + 1);
            // before commit e1e1da23f0d4a4bebdad4db3267aa0c594d52c64 there is a bug in gc name
            if (installPathByName.contains("VolumeInventory@")) {
                continue;
            }

            SharedBlockDeleteVolumeGC context = JSONObjectUtil.toObject(gc.getContext(), SharedBlockDeleteVolumeGC.class);
            if (!installPathByName.equals(context.volume.getInstallPath())) {
                logger.warn(String.format("find suspicious sharedblock gc job[uuid: %s]! " +
                        "install path from name: %s, install path from context: %s",
                        gc.getUuid(), installPathByName, context.volume.getInstallPath()));
                gc.setStatus(GCStatus.Done);
                dbf.update(gc);
            }
        }
    }
}
