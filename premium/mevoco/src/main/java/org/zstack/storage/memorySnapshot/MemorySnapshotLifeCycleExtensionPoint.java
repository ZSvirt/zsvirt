package org.zstack.storage.memorySnapshot;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupInventory;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataArchiveVO;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataManager;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataVO;
import org.zstack.header.volume.*;
import org.zstack.storage.snapshot.group.RevertVmFromSnapShotGroupExtension;
import org.zstack.storage.snapshot.group.VolumeSnapshotGroupConstant;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.multiErr;

public class MemorySnapshotLifeCycleExtensionPoint implements
        VolumeSnapshotCreationExtensionPoint,
        RevertVmFromSnapShotGroupExtension
{
    private static final CLogger logger = Utils.getLogger(MemorySnapshotLifeCycleExtensionPoint.class);

    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private VmInstanceResourceMetadataManager vidm;
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public boolean needRunExtension() {
        return true;
    }

    @Override
    public Flow getBeforeRevertFlow() {
        return new Flow() {
            @Override
            public boolean skip(Map data) {
                VolumeSnapshotGroupInventory inventory = (VolumeSnapshotGroupInventory) data.get(VolumeSnapshotGroupConstant.Parmas.SnapshotGroup.toString());
                return inventory == null || inventory.getVolumeSnapshotRefs()
                        .stream()
                        .noneMatch(ref -> ref.getVolumeType()
                                .equals(VolumeType.Memory.toString()));
            }

            final List<MemorySnapshotResourceExtensionPoint> successRecoverResourceExtensionPoint = new ArrayList<>();
            List<VmInstanceResourceMetadataVO> currentVmInstanceResourceMetadataVOs = new ArrayList<>();

            @Override
            public void run(FlowTrigger trigger, Map data) {
                VolumeSnapshotGroupInventory inventory = (VolumeSnapshotGroupInventory) data.get(VolumeSnapshotGroupConstant.Parmas.SnapshotGroup.toString());
                currentVmInstanceResourceMetadataVOs = ResourceMetadataBuilderFactory.getCurrentVmInstanceResourceMetadataVOs(inventory.getVmInstanceUuid());
                new While<>(pluginRgty.getExtensionList(MemorySnapshotResourceExtensionPoint.class)).each((ext, compl) -> {
                    List<VmInstanceResourceMetadataArchiveVO> archiveVmInfos = vidm.
                            getArchivedResourceMetadataInfoFromArchiveForResourceUuid(
                                    inventory.getVmInstanceUuid(),
                                    inventory.getUuid(),
                                    ext.getArchiveBundleCanonicalName()
                            );
                    List<Object> bundles = archiveVmInfos
                            .stream()
                            .map(archiveVmInfo -> JSONObjectUtil.
                                    toObject(archiveVmInfo.getMetadata(),
                                            ext.getArchiveBundleClass()
                                    )
                            ).collect(Collectors.toList());
                    // empty-bundle handling is delegated to each implementation;
                    ext.recoverDeviceByAddress(
                            inventory.getVmInstanceUuid(),
                            inventory.getUuid(),
                            bundles, new Completion(compl) {
                                @Override
                                public void success() {
                                    successRecoverResourceExtensionPoint.add(ext);
                                    compl.done();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    compl.addError(errorCode);
                                    compl.allDone();
                                }
                            });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errorCodeList.getCauses().isEmpty()) {
                            trigger.next();
                            return;
                        }
                        trigger.fail(errorCodeList.getCauses().get(0));
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                VolumeSnapshotGroupInventory inventory = (VolumeSnapshotGroupInventory) data.get(VolumeSnapshotGroupConstant.Parmas.SnapshotGroup.toString());
                Map<String, VmInstanceResourceMetadataVO> currentDeviceAddressByResourceUuid =
                        currentVmInstanceResourceMetadataVOs.stream().collect(Collectors.toMap(
                                VmInstanceResourceMetadataVO::getResourceUuid, address -> address));
                new While<>(successRecoverResourceExtensionPoint).each((ext, compl) -> {
                    ext.rollBackResourceAndConfigs(inventory.getVmInstanceUuid(), currentDeviceAddressByResourceUuid, new Completion(compl) {
                        @Override
                        public void success() {
                            compl.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            compl.addError(errorCode);
                            compl.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errorCodeList.getCauses().isEmpty()) {
                            logger.warn(String.format("failed to rollback memory snapshot, the vm may not be able to start, " +
                                    "please check the error log for more details: %s", multiErr(errorCodeList).getReadableDetails()));
                        }
                        trigger.rollback();
                    }
                });
            }
        };
    }

    @Override
    public void afterVolumeLiveSnapshotGroupCreatedOnBackend(CreateVolumesSnapshotOverlayInnerMsg msg, TakeVolumesSnapshotOnKvmReply treply, Completion completion) {
        completion.success();
    }

    @Override
    public void afterVolumeLiveSnapshotGroupCreationFailsOnBackend(CreateVolumesSnapshotOverlayInnerMsg msg, TakeVolumesSnapshotOnKvmReply treply) {

    }

    @Override
    public void afterVolumeSnapshotGroupCreated(VolumeSnapshotGroupInventory snapshotGroup, ConsistentType consistentType, Completion completion) {
        if (!consistentType.equals(ConsistentType.Application)) {
            completion.success();
            return;
        }
        new While<>(pluginRgty.getExtensionList(MemorySnapshotResourceExtensionPoint.class)).each((ext, wc) -> {
            ext.archiveDeviceAddressByResources(snapshotGroup.getVmInstanceUuid(), new Completion(wc) {
                @Override
                public void success() {
                    wc.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    wc.addError(errorCode);
                    wc.allDone();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.getCauses().isEmpty()) {
                    vidm.archiveCurrentResourceMetadata(snapshotGroup.getVmInstanceUuid(), snapshotGroup.getUuid());
                    completion.success();
                    return;
                }

                completion.fail(errorCodeList.getCauses().get(0));
            }
        });
    }

    @Override
    public void afterVolumeSnapshotCreated(VolumeSnapshotInventory snapshot, Completion completion) {
        completion.success();
    }

    @Override
    public List<Flow> beforeCreateVolumeSnapshotFlow(CreateVolumeSnapshotGroupMessage msg) {
        return Arrays.asList(new Flow() {
            String __name__ = "create-memory-volume-if-with-memory";

            VolumeInventory memoryVolume = null;

            @Override
            public boolean skip(Map data) {
                return msg.getConsistentType() != ConsistentType.Application || Q.New(VolumeVO.class)
                        .eq(VolumeVO_.vmInstanceUuid, msg.getVmInstance().getUuid())
                        .eq(VolumeVO_.type, VolumeType.Memory)
                        .isExists();
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                CreateVolumeMsg cmsg = new CreateVolumeMsg();
                cmsg.setAccountUuid(msg.getSession().getAccountUuid());
                cmsg.setSize(0);
                cmsg.setVmInstanceUuid(msg.getVmInstance().getUuid());
                cmsg.setPrimaryStorageUuid(msg.getVmInstance().getRootVolume().getPrimaryStorageUuid());
                cmsg.setName(String.format("memory-volume-of-vm-%s", msg.getVmInstance().getUuid()));
                cmsg.setVolumeType(VolumeType.Memory.toString());
                cmsg.setFormat(msg.getVmInstance().getRootVolume().getFormat());

                bus.makeLocalServiceId(cmsg, VolumeConstant.SERVICE_ID);
                bus.send(cmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        CreateVolumeReply r = reply.castReply();
                        memoryVolume = r.getInventory();
                        trigger.next();
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                if (memoryVolume == null) {
                    trigger.rollback();
                    return;
                }

                DeleteVolumeMsg dmsg = new DeleteVolumeMsg();
                dmsg.setDetachBeforeDeleting(false);
                dmsg.setUuid(memoryVolume.getUuid());
                dmsg.setDeletionPolicy(VolumeDeletionPolicyManager.VolumeDeletionPolicy.Direct.toString());
                bus.makeTargetServiceIdByResourceUuid(dmsg, VolumeConstant.SERVICE_ID, memoryVolume.getUuid());
                bus.send(dmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if(!reply.isSuccess()) {
                            logger.debug(String.format("failed to delete volume[uuid: %s]", memoryVolume.getUuid()));
                        }
                        trigger.rollback();
                    }
                });
            }
        }, new NoRollbackFlow() {
            String __name__ = "instantiate-memory-volume-if-with-memory";

            VolumeVO volume;

            @Override
            public boolean skip(Map data) {
                if (msg.getConsistentType() != ConsistentType.Application) {
                    return true;
                }

                volume = Q.New(VolumeVO.class)
                        .eq(VolumeVO_.vmInstanceUuid, msg.getVmInstance().getUuid())
                        .eq(VolumeVO_.type, VolumeType.Memory).find();
                return volume == null || volume.getStatus().equals(VolumeStatus.Ready);
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                InstantiateMemoryVolumeMsg imsg = new InstantiateMemoryVolumeMsg();
                imsg.setHostUuid(msg.getVmInstance().getHostUuid());
                imsg.setPrimaryStorageUuid(msg.getVmInstance().getRootVolume().getPrimaryStorageUuid());
                imsg.setVolumeUuid(volume.getUuid());
                bus.makeTargetServiceIdByResourceUuid(imsg, VolumeConstant.SERVICE_ID, imsg.getVolumeUuid());
                bus.send(imsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        volume.setInstallPath(((InstantiateVolumeReply) reply).getVolume().getInstallPath());
                        volume.setDeviceId(VmInstanceResourceMetadataManager.MEMORY_VOLUME_DEVICE_ID);
                        volume.setStatus(VolumeStatus.Ready);
                        dbf.updateAndRefresh(volume);

                        trigger.next();
                    }
                });
            }
        });
    }
}
