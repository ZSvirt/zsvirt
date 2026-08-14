package org.zstack.storage.primary.sharedblock.migration;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.job.JobQueueFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.primary.DeleteVolumeBitsOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.DeleteVolumeBitsOnPrimaryStorageReply;
import org.zstack.header.storage.primary.IncreasePrimaryStorageCapacityMsg;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.volume.VolumeFormat;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.storage.migration.StorageMigrationBase;
import org.zstack.storage.migration.StorageMigrationConstant;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;

/**
 * Create by weiwang at 2018/6/28
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SblkToSblkRollbackMigrateVolumeFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(SblkToSblkRollbackMigrateVolumeFlow.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected RESTFacade restf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected JobQueueFacade jobf;
    @Autowired
    protected ApiTimeoutManager timeoutMgr;
    @Autowired
    protected StorageMigrationBase storageMigrationService;

    @Override
    public void run(FlowTrigger flowTrigger, Map data) {
        // TODO(weiw): Fix progress
        String volumeUuid = (String) data.get(StorageMigrationConstant.VOLUME_UUID);
        String srcPsUuid = (String) data.get(StorageMigrationConstant.SRC_PS_UUID);
        String dstPsUuid = (String) data.get(StorageMigrationConstant.DST_PS_UUID);
        boolean withSnapshots = (boolean) data.get(StorageMigrationConstant.WITH_SNAPSHOTS);
        boolean discardSource = (boolean) data.get(StorageMigrationConstant.DISCARD_SOURCE);
        boolean discardDestination = (boolean) data.get(StorageMigrationConstant.DISCARD_DESTINATION);

        if (discardSource) {
            flowTrigger.fail( Platform.operr("data on source ps[uuid: %s] has been discarded, not support rollback", srcPsUuid));
            return;
        }

        VolumeVO volumeVO = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, volumeUuid).find();
        volumeVO.setInstallPath(volumeVO.getInstallPath().replace(
                dstPsUuid, srcPsUuid));
        volumeVO.setPrimaryStorageUuid(srcPsUuid);
        volumeVO.setStatus(VolumeStatus.Ready);
        dbf.update(volumeVO);
        logger.debug(String.format("rollback volume %s data in database finished", volumeVO.getUuid()));

        if (withSnapshots) {
            List<VolumeSnapshotVO> snapshotVOS = Q.New(VolumeSnapshotVO.class)
                    .eq(VolumeSnapshotVO_.volumeUuid, volumeUuid)
                    .list();
            if (snapshotVOS != null && !snapshotVOS.isEmpty()) {
                for (VolumeSnapshotVO snapshotVO : snapshotVOS) {
                    snapshotVO.setPrimaryStorageInstallPath(
                            snapshotVO.getPrimaryStorageInstallPath().replace(
                                    dstPsUuid, srcPsUuid)
                    );
                    dbf.update(snapshotVO);
                    logger.debug(String.format("rollback volume %s snapshot [%s] install path[%s] in database finished",
                            volumeVO.getUuid(), snapshotVO.getUuid(), snapshotVO.getPrimaryStorageInstallPath()));
                }
            }
        }

        if (!discardDestination) {
            logger.debug(String.format("discardDestination is [%s], skip to discard on target primary storage[%s]", discardDestination, dstPsUuid));
            flowTrigger.next();
            return;
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-volume-%s-on-ps-%s", volumeUuid, dstPsUuid));
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                DeleteVolumeBitsOnPrimaryStorageMsg msg = new DeleteVolumeBitsOnPrimaryStorageMsg();
                msg.setPrimaryStorageUuid(dstPsUuid);
                msg.setInstallPath(volumeVO.getInstallPath().replace(
                        srcPsUuid, dstPsUuid));
                msg.setHypervisorType(VolumeFormat.getMasterHypervisorTypeByVolumeFormat(volumeVO.getFormat()).toString());
                msg.setFolder(false);
                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, dstPsUuid);
                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            logger.info(String.format("Deleted volume %s in Trash.", msg.getInstallPath()));

                            IncreasePrimaryStorageCapacityMsg imsg = new IncreasePrimaryStorageCapacityMsg();
                            imsg.setPrimaryStorageUuid(dstPsUuid);
                            imsg.setDiskSize(volumeVO.getSize());
                            bus.makeTargetServiceIdByResourceUuid(imsg, PrimaryStorageConstant.SERVICE_ID, srcPsUuid);
                            bus.send(imsg);
                            logger.info(String.format("Returned space[size:%s] to PS %s after cleanup Trash", volumeVO.getSize(), srcPsUuid));
                        } else {
                            logger.warn(String.format("Failed to delete volume %s after cleanup Trash.", msg.getInstallPath()));
                        }
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (!withSnapshots) {
                    trigger.next();
                    return;
                }

                List<VolumeSnapshotVO> snapshotVOS = Q.New(VolumeSnapshotVO.class)
                        .eq(VolumeSnapshotVO_.volumeUuid, volumeUuid)
                        .list();
                if (snapshotVOS == null || snapshotVOS.isEmpty()) {
                    trigger.next();
                    return;
                }

                new While<>(snapshotVOS).all((snapshotVO, whileCompletion) -> {
                    DeleteVolumeBitsOnPrimaryStorageMsg msg = new DeleteVolumeBitsOnPrimaryStorageMsg();
                    msg.setPrimaryStorageUuid(dstPsUuid);
                    msg.setInstallPath(snapshotVO.getPrimaryStorageInstallPath().replace(
                            srcPsUuid, dstPsUuid));
                    msg.setHypervisorType(VolumeFormat.getMasterHypervisorTypeByVolumeFormat(volumeVO.getFormat()).toString());
                    msg.setFolder(false);
                    bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, srcPsUuid);
                    bus.send(msg, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                whileCompletion.done();
                                return;
                            }

                            DeleteVolumeBitsOnPrimaryStorageReply reply1 = reply.castReply();
                            if (reply1.isSuccess()) {
                                IncreasePrimaryStorageCapacityMsg imsg = new IncreasePrimaryStorageCapacityMsg();
                                imsg.setPrimaryStorageUuid(dstPsUuid);
                                imsg.setDiskSize(snapshotVO.getSize());
                                bus.makeTargetServiceIdByResourceUuid(imsg, PrimaryStorageConstant.SERVICE_ID, dstPsUuid);
                                bus.send(imsg);
                                logger.info(String.format("Returned space[size:%s] to PS %s after volume migration", snapshotVO.getSize(), dstPsUuid));
                            }
                            whileCompletion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
                    }
                });
            }
        }).done(new FlowDoneHandler(flowTrigger) {
            @Override
            public void handle(Map data) {
                flowTrigger.next();
            }
        }).error(new FlowErrorHandler(flowTrigger) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                flowTrigger.next();
            }
        }).start();
    }
}
