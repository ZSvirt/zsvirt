package org.zstack.storage.migration.primary.nfs;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.trash.StorageTrash;
import org.zstack.core.trash.TrashType;
import org.zstack.header.core.trash.InstallPathRecycleInventory;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.storage.migration.StorageMigrationConstant;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Created by GuoYi on 11/26/17.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class NfsDeleteVolumeFromSrcPSFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(NfsDeleteVolumeFromSrcPSFlow.class);

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    private StorageTrash trash;

    private static final String TRASH_IDS = "trash_ids";

    @Override
    public boolean skip(Map data) {
        boolean discardSource = (boolean) data.get(StorageMigrationConstant.DISCARD_SOURCE);
        return !discardSource;
    }

    @Override
    public void run(FlowTrigger trigger, Map data) {
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

        data.put(TRASH_IDS, trashIds);
        trigger.next();
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        if (data.get(TRASH_IDS) != null) {
            List<Long> trashIds = (List<Long>) data.get(TRASH_IDS);
            trashIds.forEach(trash::removeFromDb);
        }
        trigger.rollback();
    }
}
