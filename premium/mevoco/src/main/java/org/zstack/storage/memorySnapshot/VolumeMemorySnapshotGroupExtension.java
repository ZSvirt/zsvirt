package org.zstack.storage.memorySnapshot;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.ArchiveVolumeBundle;
import org.zstack.header.vm.AttachDataVolumeToVmMsg;
import org.zstack.header.vm.DetachDataVolumeFromVmMsg;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.devices.DeviceAddress;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataArchiveVO;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataManager;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataVO;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.storage.memorySnapshot.MemorySnapshotGroupConfigsUtils.getSystemTagsForArchive;
import static org.zstack.storage.memorySnapshot.MemorySnapshotGroupConfigsUtils.getVolumeResourceConfigsForArchive;

/**
 * Created by LiangHanYu on 2022/10/10 17:25
 */
public class VolumeMemorySnapshotGroupExtension implements MemorySnapshotResourceExtensionPoint, ResourceMetadataBuilder {
    private static final CLogger logger = Utils.getLogger(VolumeMemorySnapshotGroupExtension.class);
    @Autowired
    private VmInstanceResourceMetadataManager vidm;
    @Autowired
    private CloudBus bus;

    @Override
    public void archiveDeviceAddressByResources(String vmInstanceUuid, Completion completion) {
        List<VolumeVO> volumeVOS = Q.New(VolumeVO.class)
                .eq(VolumeVO_.vmInstanceUuid, vmInstanceUuid)
                .notEq(VolumeVO_.type, VolumeType.Memory).list();
        if (volumeVOS.isEmpty()) {
            completion.success();
            return;
        }
        volumeVOS.forEach(volumeVO -> {
            ArchiveVolumeBundle archiveVolumeBundle = new ArchiveVolumeBundle();
            archiveVolumeBundle.setVolumeInventory(VolumeInventory.valueOf(volumeVO));
            archiveVolumeBundle.setResourceConfigBundles(getVolumeResourceConfigsForArchive(volumeVO.getUuid()));
            archiveVolumeBundle.setSystemTagBundles(getSystemTagsForArchive(volumeVO.getUuid()));

            vidm.createOrUpdateVmResourceMetadata(volumeVO.getUuid(),
                    null,
                    vmInstanceUuid,
                    JSONObjectUtil.toJsonString(archiveVolumeBundle),
                    ArchiveVolumeBundle.class.getCanonicalName());
        });
        completion.success();
    }

    public static List<VmInstanceResourceMetadataVO> buildCurrentVolumeResourceMetadataVOs(String vmInstanceUuid) {
        List<VolumeVO> volumeVOS = Q.New(VolumeVO.class)
                .eq(VolumeVO_.vmInstanceUuid, vmInstanceUuid)
                .notEq(VolumeVO_.type, VolumeType.Memory).list();
        if (volumeVOS.isEmpty()) {
            return Collections.emptyList();
        }
        List<VmInstanceResourceMetadataVO> vos = new ArrayList<>();
        volumeVOS.forEach(volumeVO -> {
            VmInstanceResourceMetadataVO vo = new VmInstanceResourceMetadataVO();
            vo.setVmInstanceUuid(vmInstanceUuid);
            vo.setResourceUuid(volumeVO.getUuid());
            vo.setMetadataClass(ArchiveVolumeBundle.class.getCanonicalName());

            ArchiveVolumeBundle archiveVolumeBundle = new ArchiveVolumeBundle();
            archiveVolumeBundle.setVolumeInventory(VolumeInventory.valueOf(volumeVO));
            archiveVolumeBundle.setResourceConfigBundles(getVolumeResourceConfigsForArchive(volumeVO.getUuid()));
            archiveVolumeBundle.setSystemTagBundles(getSystemTagsForArchive(volumeVO.getUuid()));
            vo.setMetadata(JSONObjectUtil.toJsonString(archiveVolumeBundle));
            vos.add(vo);
        });
        return vos;
    }

    @Override
    public void recoverDeviceByAddress(String vmInstanceUuid, String resourceUuid, List<?> bundles, Completion completion) {
        if (bundles.isEmpty()) {
            completion.success();
            return;
        }

        List<VmInstanceResourceMetadataArchiveVO> archiveVolumeList = vidm.
                getArchivedResourceMetadataInfoFromArchiveForResourceUuid(vmInstanceUuid,
                        resourceUuid,
                        getArchiveBundleCanonicalName())
                .stream()
                .sorted(Comparator.comparingInt(needToRevertVolume -> ((ArchiveVolumeBundle) (JSONObjectUtil.toObject(needToRevertVolume.getMetadata(), getArchiveBundleClass()))).getVolumeInventory().getDeviceId()))
                .collect(Collectors.toList());

        volumesAndConfigsRestoreFlow(vmInstanceUuid, archiveVolumeList, completion);
    }

    private void volumesAndConfigsRestoreFlow(String vmInstanceUuid, List<VmInstanceResourceMetadataArchiveVO> archiveVolumeList, Completion completion) {
        List<String> archiveVolumeUuidList = archiveVolumeList.stream().map(VmInstanceResourceMetadataArchiveVO::getResourceUuid).collect(Collectors.toList());

        List<VolumeVO> currentVolumeList = Q.New(VolumeVO.class).eq(VolumeVO_.vmInstanceUuid, vmInstanceUuid).notEq(VolumeVO_.type, VolumeType.Memory).list();
        List<String> currentVolumeUuidList = currentVolumeList.stream().map(ResourceVO::getUuid).collect(Collectors.toList());

        List<VolumeVO> needDetachVolumes = currentVolumeList.stream().filter(volumeVO -> !archiveVolumeUuidList.contains(volumeVO.getUuid())).collect(Collectors.toList());
        List<VmInstanceResourceMetadataArchiveVO> needAttachVolumes = archiveVolumeList.stream()
                .filter(VmInstanceResourceMetadataArchiveVO ->
                        !currentVolumeUuidList.contains(VmInstanceResourceMetadataArchiveVO.getResourceUuid())).collect(Collectors.toList());

        FlowChain fchain = FlowChainBuilder.newShareFlowChain();
        fchain.setName(String.format("revert-vm-%s-volume-info", vmInstanceUuid));
        fchain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "detach-volume-by-memory-snapshot-group";

                    @Override
                    public boolean skip(Map data) {
                        return needDetachVolumes.isEmpty();
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new While<>(needDetachVolumes).each((volumeVO, whileCompletion) -> {
                            DetachDataVolumeFromVmMsg dmsg = new DetachDataVolumeFromVmMsg();
                            dmsg.setVmInstanceUuid(vmInstanceUuid);
                            dmsg.setVolume(VolumeInventory.valueOf(volumeVO));
                            bus.makeTargetServiceIdByResourceUuid(dmsg, VmInstanceConstant.SERVICE_ID, dmsg.getVmInstanceUuid());
                            bus.send(dmsg, new CloudBusCallBack(whileCompletion) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        whileCompletion.addError(reply.getError());
                                        whileCompletion.allDone();
                                        return;
                                    }
                                    whileCompletion.done();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (errorCodeList.getCauses().isEmpty()) {
                                    trigger.next();
                                } else {
                                    trigger.fail(errorCodeList.getCauses().get(0));
                                }
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "attach-volume-by-memory-snapshot-group";

                    @Override
                    public boolean skip(Map data) {
                        return needAttachVolumes.isEmpty();
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new While<>(archiveVolumeList).each((archiveVolume, whileCompletion) -> {
                            VolumeInventory volumeInventory = ((ArchiveVolumeBundle) (JSONObjectUtil.toObject(archiveVolume.getMetadata(), getArchiveBundleClass()))).getVolumeInventory();
                            // since there was no previous detach, lastVmInstanceUuid is set to the current vmInstanceUuid.
                            if (volumeInventory.getLastVmInstanceUuid() == null) {
                                volumeInventory.setLastVmInstanceUuid(volumeInventory.getVmInstanceUuid());
                            }
                            AttachDataVolumeToVmMsg amsg = new AttachDataVolumeToVmMsg();
                            amsg.setVmInstanceUuid(vmInstanceUuid);
                            amsg.setVolume(volumeInventory);
                            bus.makeTargetServiceIdByResourceUuid(amsg, VmInstanceConstant.SERVICE_ID, amsg.getVmInstanceUuid());
                            bus.send(amsg, new CloudBusCallBack(whileCompletion) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        whileCompletion.addError(reply.getError());
                                        whileCompletion.allDone();
                                        return;
                                    }
                                    whileCompletion.done();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (errorCodeList.getCauses().isEmpty()) {
                                    trigger.next();
                                } else {
                                    trigger.fail(errorCodeList.getCauses().get(0));
                                }
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "revert-configs-from-archive";

                    @Override
                    public boolean skip(Map data) {
                        return archiveVolumeList.isEmpty();
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        archiveVolumeList.forEach(vo -> {
                            VolumeInventory volume = JSONObjectUtil.toObject(vo.getMetadata(), ArchiveVolumeBundle.class).getVolumeInventory();
                            logger.info(String.format("update lastAttachDate[%s] and deviceId[%d] and volumeQos[%s] " +
                                    "for volume[uuid:%s] from  archive", volume.getLastAttachDate(), volume.getDeviceId(), volume.getVolumeQos(), volume.getUuid()));
                            SQL.New(VolumeVO.class).eq(VolumeVO_.uuid, volume.getUuid())
                                    .set(VolumeVO_.lastAttachDate, volume.getLastAttachDate())
                                    .set(VolumeVO_.deviceId, volume.getDeviceId())
                                    .set(VolumeVO_.volumeQos, volume.getVolumeQos())
                                    .update();

                            ArchiveVolumeBundle archiveVolumeBundle = JSONObjectUtil.toObject(vo.getMetadata(), ArchiveVolumeBundle.class);
                            MemorySnapshotGroupConfigsUtils.restoreConfigs(vo.getResourceUuid(), archiveVolumeBundle);

                            logger.debug(String.format("update device address for resourceUuid[uuid:%s], vmInstanceUuid[uuid:%s], new deviceAddress[%s]",
                                    vo.getResourceUuid(), vmInstanceUuid, vo.getDeviceAddress()));
                            vidm.createOrUpdateVmResourceMetadata(vo.getResourceUuid(),
                                    DeviceAddress.fromString(vo.getDeviceAddress()), vmInstanceUuid,
                                    vo.getMetadata(), vo.getMetadataClass());
                        });
                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    public void rollBackResourceAndConfigs(String vmInstanceUuid, Map<String, VmInstanceResourceMetadataVO> originDeviceAddressByResourceUuid, Completion completion) {
        List<VmInstanceResourceMetadataArchiveVO> archiveVolumeList = originDeviceAddressByResourceUuid.values()
                .stream()
                .filter(vo -> Objects.equals(vo.getMetadataClass(), ArchiveVolumeBundle.class.getCanonicalName()))
                .map(vo -> {
                    VmInstanceResourceMetadataArchiveVO archiveVO = new VmInstanceResourceMetadataArchiveVO();
                    archiveVO.setMetadata(vo.getMetadata());
                    archiveVO.setMetadataClass(vo.getMetadataClass());
                    archiveVO.setResourceUuid(vo.getResourceUuid());
                    archiveVO.setDeviceAddress(vo.getDeviceAddress());
                    return archiveVO;
                })
                .sorted(Comparator.comparingInt(needToRevertVolume -> ((JSONObjectUtil.toObject(needToRevertVolume.getMetadata(), ArchiveVolumeBundle.class))).getVolumeInventory().getDeviceId()))
                .collect(Collectors.toList());

        volumesAndConfigsRestoreFlow(vmInstanceUuid, archiveVolumeList, completion);
    }

    @Override
    public String getArchiveBundleCanonicalName() {
        return ArchiveVolumeBundle.class.getCanonicalName();
    }

    @Override
    public Class getArchiveBundleClass() {
        return ArchiveVolumeBundle.class;
    }

    @Override
    public List<VmInstanceResourceMetadataVO> buildResourceMetadataVOs(String vmInstanceUuid) {
        return buildCurrentVolumeResourceMetadataVOs(vmInstanceUuid);
    }
}
