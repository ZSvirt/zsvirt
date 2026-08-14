package org.zstack.storage.migration.primary.ceph;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.core.trash.StorageTrash;
import org.zstack.core.trash.TrashType;
import org.zstack.header.core.trash.InstallPathRecycleInventory;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.migration.StorageMigrationConstant;
import org.zstack.storage.snapshot.reference.VolumeSnapshotReferenceUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by GuoYi on 10/4/17.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CephDeleteVolumeFromSrcPSFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(CephDeleteVolumeFromSrcPSFlow.class);

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected ApiTimeoutManager timeoutMgr;
    @Autowired
    private StorageTrash trash;

    private static final String TRASH_VOLUMES = "trash_volumes";

    @Override
    public boolean skip(Map data) {
        boolean discardSource = (boolean) data.get(StorageMigrationConstant.DISCARD_SOURCE);
        return !discardSource;
    }

    @Override
    public void run(FlowTrigger trigger, Map data) {
        // TODO: refactor the source volume install paths logic
        Set<String> srcVolumeInstallPaths = (Set<String>)data.get(StorageMigrationConstant.SRC_VOLUME_INSTALL_PATHS);
        ArrayList<Long> trashVols = new ArrayList<>();
        List objs = (List) data.get(StorageMigrationConstant.OBJECTS_TO_TRASH);
        if (CollectionUtils.isEmpty(objs)) {
            trigger.next();
            return;
        }

        for (Object obj : objs) {
            // ceph do not trash snapshot
            if (obj instanceof VolumeInventory && srcVolumeInstallPaths.contains(((VolumeInventory) obj).getInstallPath())) {
                InstallPathRecycleInventory trashInv = trash.createTrash(TrashType.MigrateVolume, false, obj);
                trashVols.add(trashInv.getTrashId());
            }
        }

        data.put(TRASH_VOLUMES, trashVols);
        trigger.next();
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        ArrayList<Long> trashIds = (ArrayList<Long>) data.get(TRASH_VOLUMES);
        if (trashIds != null) {
            trashIds.forEach(trash::removeFromDb);
        }
        trigger.rollback();
    }
}
