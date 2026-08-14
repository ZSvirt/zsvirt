package org.zstack.storage.memorySnapshot;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.image.ImageState;
import org.zstack.header.image.ImageStatus;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.ArchiveVmCdRomBundle;
import org.zstack.header.vm.CreateVmCdRomMsg;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.cdrom.DeleteVmCdRomMsg;
import org.zstack.header.vm.cdrom.VmCdRomInventory;
import org.zstack.header.vm.cdrom.VmCdRomVO;
import org.zstack.header.vm.cdrom.VmCdRomVO_;
import org.zstack.header.vm.devices.DeviceAddress;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataArchiveVO;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataManager;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataVO;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.header.vm.ArchiveVmCdRomBundle.toDeleteVmCdRomMsg;

/**
 * Created by LiangHanYu on 2022/6/20 11:41
 */
public class VmCdRomMemorySnapshotGroupExtension implements MemorySnapshotResourceExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VmCdRomMemorySnapshotGroupExtension.class);

    @Autowired
    private CloudBus bus;

    @Autowired
    private VmInstanceResourceMetadataManager vidm;

    @Override
    public void archiveDeviceAddressByResources(String vmInstanceUuid, Completion completion) {
        List<VmCdRomVO> cdRomVOS = Q.New(VmCdRomVO.class)
                .eq(VmCdRomVO_.vmInstanceUuid, vmInstanceUuid)
                .list();
        if (cdRomVOS.isEmpty()) {
            completion.success();
            return;
        }

        for (VmCdRomVO cdRomVO : cdRomVOS) {
            vidm.createOrUpdateVmResourceMetadata(cdRomVO.getUuid(),
                    null,
                    cdRomVO.getVmInstanceUuid(),
                    JSONObjectUtil.toJsonString(new ArchiveVmCdRomBundle(VmCdRomInventory.valueOf(cdRomVO))),
                    getArchiveBundleCanonicalName());
        }
        completion.success();
    }

    @Override
    public void recoverDeviceByAddress(String vmInstanceUuid, String resourceUuid, List<?> bundles, Completion completion) {
        List<VmInstanceResourceMetadataArchiveVO> needToRevertCdRomList = vidm.
                getArchivedResourceMetadataInfoFromArchiveForResourceUuid(vmInstanceUuid,
                        resourceUuid,
                        getArchiveBundleCanonicalName())
                .stream()
                .sorted(Comparator.comparing(needToRevertCdRom ->
                        Optional.of(((ArchiveVmCdRomBundle) JSONObjectUtil.
                                        toObject(needToRevertCdRom.getMetadata(),
                                                getArchiveBundleClass()))
                                        .getCdRomInventory().getDeviceId())
                                .orElse(Integer.MAX_VALUE)
                ))
                .collect(Collectors.toList());

        cdRomsAndConfigsRestoreFlow(vmInstanceUuid, needToRevertCdRomList, completion);
    }

    private void cdRomsAndConfigsRestoreFlow(String vmInstanceUuid, List<VmInstanceResourceMetadataArchiveVO> needRestoreCdRoms, Completion completion) {
        FlowChain fchain = FlowChainBuilder.newShareFlowChain();
        fchain.setName(String.format("revert-vm-%s-cdRom-info", vmInstanceUuid));
        fchain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-all-cdRoms";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<String> needToDeleteVmCdRomUuidListCurrently = Q.New(VmCdRomVO.class).select(VmCdRomVO_.uuid).eq(VmCdRomVO_.vmInstanceUuid, vmInstanceUuid).listValues();
                        new While<>(needToDeleteVmCdRomUuidListCurrently).step((cdRomUuid, whileCompletion) -> {
                            DeleteVmCdRomMsg dmsg = toDeleteVmCdRomMsg(vmInstanceUuid, cdRomUuid);
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
                        }, 10).run(new WhileDoneCompletion(trigger) {
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
                    String __name__ = "create-cdRoms-saved-by-memory-snapshot-group";

                    private boolean isCdRomIsoEnabled(String isoUuid) {
                        return Q.New(ImageVO.class).eq(ImageVO_.uuid, isoUuid)
                                .eq(ImageVO_.state, ImageState.Enabled)
                                .eq(ImageVO_.status, ImageStatus.Ready)
                                .isExists();
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new While<>(needRestoreCdRoms).each((archiveVO, whileCompletion) -> {
                            VmCdRomInventory cdRomInventory = JSONObjectUtil.toObject(archiveVO.getMetadata(), ArchiveVmCdRomBundle.class).getCdRomInventory();
                            CreateVmCdRomMsg cmsg = ArchiveVmCdRomBundle.toCreateVmCdRomMsg(vmInstanceUuid, archiveVO.getMetadata());
                            if (isCdRomIsoEnabled(cdRomInventory.getIsoUuid())) {
                                cmsg.setIsoUuid(cdRomInventory.getIsoUuid());
                            }
                            cmsg.setDescription(cdRomInventory.getDescription());
                            bus.makeTargetServiceIdByResourceUuid(cmsg, VmInstanceConstant.SERVICE_ID, cmsg.getVmInstanceUuid());
                            bus.send(cmsg, new CloudBusCallBack(whileCompletion) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        whileCompletion.addError(reply.getError());
                                        whileCompletion.allDone();
                                        return;
                                    }

                                    logger.debug(String.format("update device address for resourceUuid[uuid:%s], vmInstanceUuid[uuid:%s], new deviceAddress[%s]",
                                            archiveVO.getResourceUuid(), vmInstanceUuid, archiveVO.getDeviceAddress()));
                                    vidm.createOrUpdateVmResourceMetadata(cdRomInventory.getUuid(),
                                            DeviceAddress.fromString(archiveVO.getDeviceAddress()), cdRomInventory.getVmInstanceUuid(),
                                            archiveVO.getMetadata(), archiveVO.getMetadataClass());
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
        List<VmInstanceResourceMetadataArchiveVO> needRestoreCdRoms = originDeviceAddressByResourceUuid.values()
                .stream()
                .filter(vo -> Objects.equals(vo.getMetadataClass(), ArchiveVmCdRomBundle.class.getCanonicalName()))
                .map(vo -> {
                    VmInstanceResourceMetadataArchiveVO archiveVO = new VmInstanceResourceMetadataArchiveVO();
                    archiveVO.setMetadata(vo.getMetadata());
                    archiveVO.setMetadataClass(vo.getMetadataClass());
                    archiveVO.setResourceUuid(vo.getResourceUuid());
                    archiveVO.setDeviceAddress(vo.getDeviceAddress());
                    return archiveVO;
                })
                .sorted(Comparator.comparing(needToRevertCdRom -> Optional.of(
                        JSONObjectUtil.toObject(needToRevertCdRom.getMetadata(), ArchiveVmCdRomBundle.class).getCdRomInventory().getDeviceId()).orElse(Integer.MAX_VALUE)))
                .collect(Collectors.toList());

        cdRomsAndConfigsRestoreFlow(vmInstanceUuid, needRestoreCdRoms, completion);
    }

    @Override
    public String getArchiveBundleCanonicalName() {
        return ArchiveVmCdRomBundle.class.getCanonicalName();
    }

    @Override
    public Class getArchiveBundleClass() {
        return ArchiveVmCdRomBundle.class;
    }
}