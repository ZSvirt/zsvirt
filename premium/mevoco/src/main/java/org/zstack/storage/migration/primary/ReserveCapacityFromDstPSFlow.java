package org.zstack.storage.migration.primary;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.migration.StorageMigrationConstant;
import org.zstack.storage.primary.PrimaryStoragePhysicalCapacityManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.List;
import java.util.Map;

/**
 * Created by GuoYi on 10/4/17.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ReserveCapacityFromDstPSFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(ReserveCapacityFromDstPSFlow.class);

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected PrimaryStoragePhysicalCapacityManager physicalCapacityMgr;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        boolean withSnapshots = (boolean) data.get(StorageMigrationConstant.WITH_SNAPSHOTS);
        // get total size of source volume and all its snapshots
        String volumeUuid = (String) data.get(StorageMigrationConstant.VOLUME_UUID);
        VolumeVO volume = dbf.findByUuid(volumeUuid, VolumeVO.class);
        long totalSize = volume.getSize();

        data.put(StorageMigrationConstant.VOLUME_SIZE, totalSize);

        // get installPath of srcVolume and snapshots
        List<Tuple> srcVolumeSnapshotPaths = Q.New(VolumeSnapshotVO.class)
                .select(VolumeSnapshotVO_.uuid, VolumeSnapshotVO_.primaryStorageInstallPath)
                .eq(VolumeSnapshotVO_.volumeUuid, volumeUuid)
                .listTuple();

        Long volumeSnapshotsSize = SQL.New("select sum(vs.size) from VolumeSnapshotVO vs where vs.volumeUuid = :volumeUuid", Long.class)
                .param("volumeUuid", volumeUuid).find();
        data.put(StorageMigrationConstant.SRC_VOLUME_INSTALL_PATH, volume.getInstallPath());
        data.put(StorageMigrationConstant.SRC_VOLUME_SNAPSHOT_PATHS, srcVolumeSnapshotPaths);
        data.put(StorageMigrationConstant.VOLUME_SNAPSHOT_SIZE, 0L);

        if (withSnapshots) {
            data.put(StorageMigrationConstant.VOLUME_SNAPSHOT_SIZE, volumeSnapshotsSize != null ? volumeSnapshotsSize : 0L);
            totalSize += volumeSnapshotsSize != null ? volumeSnapshotsSize : 0;
        }

        String dstPsUuid = (String) data.get(StorageMigrationConstant.DST_PS_UUID);
        AllocatePrimaryStorageSpaceMsg amsg = new AllocatePrimaryStorageSpaceMsg();
        amsg.setRequiredPrimaryStorageUuid(dstPsUuid);
        amsg.setSize(totalSize);
        if (volume.getType() == VolumeType.Root) {
            amsg.setPurpose(PrimaryStorageAllocationPurpose.CreateRootVolume.toString());
        } else {
            amsg.setPurpose(PrimaryStorageAllocationPurpose.CreateDataVolume.toString());
        }
        amsg.setSystemTags((List<String>) data.get(StorageMigrationConstant.SYSTEM_TAGS));

        bus.makeLocalServiceId(amsg, PrimaryStorageConstant.SERVICE_ID);
        logger.info(String.format("Reserved space [%s] for volume %s in PS %s.", totalSize, volumeUuid, dstPsUuid));
        bus.send(amsg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    trigger.fail(reply.getError());
                    return;
                }
                AllocatePrimaryStorageSpaceReply ar = (AllocatePrimaryStorageSpaceReply) reply;
                data.put(StorageMigrationConstant.ALLOCATED_INSTALL_URL, ar.getAllocatedInstallUrl());
                trigger.next();
            }
        });
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        String dstPsUuid = (String) data.get(StorageMigrationConstant.DST_PS_UUID);
        long volumeSize = Long.parseLong(data.get(StorageMigrationConstant.VOLUME_SIZE).toString());
        long volumeSnapshotsSize = Long.parseLong(data.get(StorageMigrationConstant.VOLUME_SNAPSHOT_SIZE).toString());
        ReleasePrimaryStorageSpaceMsg rmsg = new ReleasePrimaryStorageSpaceMsg();
        rmsg.setAllocatedInstallUrl((String) data.get(StorageMigrationConstant.ALLOCATED_INSTALL_URL));
        rmsg.setPrimaryStorageUuid(dstPsUuid);
        rmsg.setDiskSize(volumeSize + volumeSnapshotsSize);
        bus.makeLocalServiceId(rmsg, PrimaryStorageConstant.SERVICE_ID);
        bus.send(rmsg);

        trigger.rollback();
    }
}
