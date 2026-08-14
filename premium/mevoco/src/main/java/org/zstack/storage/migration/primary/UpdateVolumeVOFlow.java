package org.zstack.storage.migration.primary;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO_;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.tag.TagType;
import org.zstack.header.volume.*;
import org.zstack.storage.migration.StorageMigrationConstant;
import org.zstack.storage.primary.PrimaryStorageProvisioningStrategyGetter;
import org.zstack.storage.volume.VolumeSystemTags;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.zstack.storage.volume.VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY_TOKEN;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by GuoYi on 10/2/17.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class UpdateVolumeVOFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(UpdateVolumeVOFlow.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        updateVolumeRelatedDbInfo(data);
        trigger.next();
    }

    @Transactional
    private void updateVolumeRelatedDbInfo(Map data) {
        String dstPsUuid = (String) data.get(StorageMigrationConstant.DST_PS_UUID);
        String dstVolumeInstallPath = (String) data.get(StorageMigrationConstant.DST_VOLUME_INSTALL_PATH);
        String volumeUuid = (String) data.get(StorageMigrationConstant.VOLUME_UUID);
        boolean withSnapshots = (boolean) data.get(StorageMigrationConstant.WITH_SNAPSHOTS);

        VolumeVO srcVolume = Q.New(VolumeVO.class)
                .eq(VolumeVO_.uuid, volumeUuid)
                .find();
        List<VolumeSnapshotVO> srcSnapshots = Q.New(VolumeSnapshotVO.class)
                .eq(VolumeSnapshotVO_.volumeUuid, volumeUuid)
                .list();
        data.put(StorageMigrationConstant.SRC_VOLUME, VolumeInventory.valueOf(srcVolume));
        data.put(StorageMigrationConstant.SRC_VOLUME_SNAPSHOTS, VolumeSnapshotInventory.valueOf(srcSnapshots));

        // update VolumeVO
        SQL.New(VolumeVO.class).eq(VolumeVO_.uuid, volumeUuid)
                .set(VolumeVO_.primaryStorageUuid, dstPsUuid)
                .set(VolumeVO_.installPath, dstVolumeInstallPath)
                .set(VolumeVO_.status, VolumeStatus.Ready)
                .update();
        logger.info(String.format("Update VolumeVO[uuid:%s] after volume migration.", volumeUuid));

        setVolumeProvisioningStrategy(volumeUuid, dstPsUuid);
        if (withSnapshots) {
            Map<String, String> dstVolumeSnapshotPaths = (Map<String, String>) data.get(StorageMigrationConstant.DST_VOLUME_SNAPSHOT_PATHS);
            if (dstVolumeSnapshotPaths == null || dstVolumeSnapshotPaths.keySet().isEmpty()) {
                logger.debug("not found snapshots, skip to update");
                return;
            }

            Set<String> snapshotUuids = dstVolumeSnapshotPaths.keySet();
            for (String uuid : snapshotUuids) {
                SQL.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, uuid)
                        .set(VolumeSnapshotVO_.primaryStorageUuid, dstPsUuid)
                        .set(VolumeSnapshotVO_.primaryStorageInstallPath, dstVolumeSnapshotPaths.get(uuid))
                        .update();
                SQL.New(VolumeSnapshotGroupRefVO.class).eq(VolumeSnapshotGroupRefVO_.volumeSnapshotUuid, uuid)
                                .set(VolumeSnapshotGroupRefVO_.volumeSnapshotInstallPath, dstVolumeSnapshotPaths.get(uuid))
                                .update();
                logger.debug(String.format("updated snapshot[%s] with new install path[%s]",
                        uuid, dstVolumeSnapshotPaths.get(uuid)));
            }
        }
    }

    private void setVolumeProvisioningStrategy(String volumeUuid, String primaryStorageUuid) {
        String volumeProvisioningStrategy = PrimaryStorageProvisioningStrategyGetter.get(primaryStorageUuid);

        SystemTagCreator tagCreator = VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY.newSystemTagCreator(volumeUuid);
        tagCreator.setTagByTokens(
                map(e(VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY_TOKEN, volumeProvisioningStrategy))
        );
        tagCreator.inherent = false;
        tagCreator.recreate = true;
        tagCreator.create();
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        rollbackVolumeRelatedDbInfo(data);
        trigger.rollback();
    }

    @Transactional
    private void rollbackVolumeRelatedDbInfo(Map data) {
        String srcPsUuid = (String) data.get(StorageMigrationConstant.SRC_PS_UUID);
        String srcVolumeInstallPath = (String) data.get(StorageMigrationConstant.SRC_VOLUME_INSTALL_PATH);
        String volumeUuid = (String) data.get(StorageMigrationConstant.VOLUME_UUID);
        boolean withSnapshots = (boolean) data.get(StorageMigrationConstant.WITH_SNAPSHOTS);

        // recover VolumeVO
        SQL.New(VolumeVO.class).eq(VolumeVO_.uuid, volumeUuid)
                .set(VolumeVO_.primaryStorageUuid, srcPsUuid)
                .set(VolumeVO_.installPath, srcVolumeInstallPath)
                .update();

        setVolumeProvisioningStrategy(volumeUuid, srcPsUuid);
        // recover VolumeSnapshotVO
        if (withSnapshots) {
            List<Tuple> srcVolumeSnapshotPaths = (List<Tuple>) data.get(StorageMigrationConstant.SRC_VOLUME_SNAPSHOT_PATHS);
            for (Tuple tuple : srcVolumeSnapshotPaths) {
                String uuid = (String) tuple.get(0);
                String snapshotPath = (String) tuple.get(1);
                SQL.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, uuid)
                        .set(VolumeSnapshotVO_.primaryStorageUuid, srcPsUuid)
                        .set(VolumeSnapshotVO_.primaryStorageInstallPath, snapshotPath)
                        .update();
                SQL.New(VolumeSnapshotGroupRefVO.class).eq(VolumeSnapshotGroupRefVO_.volumeSnapshotUuid, uuid)
                        .set(VolumeSnapshotGroupRefVO_.volumeSnapshotInstallPath, snapshotPath)
                        .update();
            }
        }
    }
}
