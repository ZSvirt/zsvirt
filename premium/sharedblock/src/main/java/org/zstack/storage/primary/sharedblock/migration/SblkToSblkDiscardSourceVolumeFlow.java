package org.zstack.storage.primary.sharedblock.migration;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.trash.StorageTrash;
import org.zstack.core.trash.TrashType;
import org.zstack.header.core.trash.InstallPathRecycleInventory;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.storage.migration.StorageMigrationConstant;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Create by weiwang at 2018/6/28
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SblkToSblkDiscardSourceVolumeFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(SblkToSblkDiscardSourceVolumeFlow.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected RESTFacade restf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    private StorageTrash trash;

    private static final String TRASH_LIST = "volume_trash_id_list";

    @Override
    public boolean skip(Map data) {
        boolean discardSource = (boolean) data.get(StorageMigrationConstant.DISCARD_SOURCE);
        return !discardSource;
    }

    @Override
    public void run(FlowTrigger trigger, Map data) {
        // TODO(weiw): Fix progress
        List objs = (List) data.get(StorageMigrationConstant.OBJECTS_TO_TRASH);
        if (CollectionUtils.isEmpty(objs)) {
            trigger.next();
            return;
        }

        List<Long> trashIds = new ArrayList<>();
        String volumeUuid = (String) data.get(StorageMigrationConstant.VOLUME_UUID);
        for (Object obj : objs) {
            if (obj instanceof VolumeSnapshotInventory) {
                InstallPathRecycleInventory trashInv = trash.createTrash(TrashType.MigrateVolumeSnapshot, false, obj);
                trashIds.add(trashInv.getTrashId());
                logger.debug(String.format("trash volume snapshot[uuid:%s] source file[path: %s]",
                        ((VolumeSnapshotInventory) obj).getUuid(), trashInv.getInstallPath()));
            } else if (obj instanceof VolumeInventory) {
                InstallPathRecycleInventory trashInv = trash.createTrash(TrashType.MigrateVolume, false, obj);
                trashIds.add(trashInv.getTrashId());
                logger.debug(String.format("trash volume[uuid:%s] source file[path: %s]", volumeUuid, trashInv.getInstallPath()));
            }
        }

        data.put(TRASH_LIST, trashIds);
        trigger.next();
    }

    @Override
    public void rollback(FlowRollback flowRollback, Map data) {
        List<Long> trashIdList = (List<Long>) data.get(TRASH_LIST);
        if (trashIdList != null) {
            trashIdList.forEach(trash::removeFromDb);
        }
        flowRollback.rollback();
    }
}
