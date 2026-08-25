package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.allocator.HostAllocatorManager;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.configuration.DiskOfferingVO_;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.image.ImageBackupStorageRefVO;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.BackupStorageVO_;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.*;
import org.zstack.header.volume.*;
import org.zstack.identity.AccountManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmInstantiateOtherDiskFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(VmInstantiateOtherDiskFlow.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    protected HostAllocatorManager hostAllocatorMgr;

    DiskAO diskAO;
    VolumeInventory volumeInventory;

    public VmInstantiateOtherDiskFlow(DiskAO diskAO) {
        this.diskAO = diskAO;
    }

    @Override
    public void run(FlowTrigger trigger, Map data) {
        VmInstanceInventory instantiateVm = (VmInstanceInventory) data.get(VmInstanceInventory.class.getSimpleName());
        String vmUuid = instantiateVm.getUuid();
        String hostUuid = instantiateVm.getLastHostUuid();
        String accountUuid = acntMgr.getOwnerAccountUuidOfResource(instantiateVm.getUuid());

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("instantiate-other-disk-from-vm-%s", vmUuid));
        chain.then(new ShareFlow() {
            String allocatedInstallUrl;
            String allocatedPrimaryStorageUuid;

            @Override
            public void setup() {
                if (diskAO.getSize() != 0) {
                    setupCreateVolumeFromDiskSizeFlows(diskAO.getSize());
                    setupAttachVolumeFlows();
                } else if (diskAO.getDiskOfferingUuid() != null) {
                    Long size = Q.New(DiskOfferingVO.class).eq(DiskOfferingVO_.uuid, diskAO.getDiskOfferingUuid())
                            .select(DiskOfferingVO_.diskSize).findValue();
                    setupCreateVolumeFromDiskSizeFlows(size);
                    setupAttachVolumeFlows();
                } else if (diskAO.getTemplateUuid() != null) {
                    setupVolumeFromTemplateUuidFlows();
                    setupAttachVolumeFlows();
                } else if (isAttachDataVolume()) {
                    VolumeVO volume = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, diskAO.getSourceUuid()).find();
                    volumeInventory = VolumeInventory.valueOf(volume);
                    setupAttachVolumeFlows();
                } else if (diskAO.getSourceUuid() != null && diskAO.getSourceType() != null) {
                    setupAttachOtherDiskFlows();
                } else {
                    trigger.fail(operr("the diskAO parameter is incorrect. need to set one of the following properties, " +
                            "and can only be one of them: size, templateUuid, diskOfferingUuid, sourceUuid-sourceType"));
                }

                done(new FlowDoneHandler(trigger) {
                    @Override
                    public void handle(Map data) {
                        trigger.next();
                    }
                });

                error(new FlowErrorHandler(trigger) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        trigger.fail(errCode);
                    }
                });
            }

            private boolean isAttachDataVolume() {
                return diskAO.getSourceUuid() != null && diskAO.getSourceType() != null && Objects.equals(diskAO.getSourceType(), VolumeVO.class.getSimpleName());
            }

            private void setupCreateVolumeFromDiskSizeFlows(Long diskSize) {
                flow(new Flow() {
                    String __name__ = "allocate-primaryStorage";

                    boolean isSuccessAllocatePS = false;

                    @Override
                    public void run(final FlowTrigger innerTrigger, Map data) {
                        AllocatePrimaryStorageSpaceMsg amsg = new AllocatePrimaryStorageSpaceMsg();
                        amsg.setSize(diskSize);
                        amsg.setVmInstanceUuid(vmUuid);
                        amsg.setRequiredHostUuid(hostUuid);
                        amsg.setSystemTags(diskAO.getSystemTags());
                        amsg.setDiskOfferingUuid(diskAO.getDiskOfferingUuid());
                        amsg.setRequiredPrimaryStorageUuid(diskAO.getPrimaryStorageUuid());
                        amsg.setPurpose(PrimaryStorageAllocationPurpose.CreateDataVolume.toString());
                        amsg.setTags(diskAO.getSystemTags());
                        if (Boolean.TRUE.equals(diskAO.getEncrypted()) && diskAO.getPrimaryStorageUuid() == null) {
                            amsg.addRequiredFeature(PrimaryStorageFeature.ENCRYPTED_VOLUME);
                        }
                        bus.makeLocalServiceId(amsg, PrimaryStorageConstant.SERVICE_ID);
                        bus.send(amsg, new CloudBusCallBack(innerTrigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    innerTrigger.fail(reply.getError());
                                    return;
                                }
                                AllocatePrimaryStorageSpaceReply ar = (AllocatePrimaryStorageSpaceReply) reply;
                                allocatedInstallUrl = ar.getAllocatedInstallUrl();
                                allocatedPrimaryStorageUuid = ar.getPrimaryStorageInventory().getUuid();
                                isSuccessAllocatePS = true;
                                innerTrigger.next();
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback chain, Map data) {
                        if (!isSuccessAllocatePS) {
                            chain.rollback();
                            return;
                        }
                        ReleasePrimaryStorageSpaceMsg msg = new ReleasePrimaryStorageSpaceMsg();
                        msg.setDiskSize(diskAO.getSize());
                        msg.setAllocatedInstallUrl(allocatedInstallUrl);
                        msg.setPrimaryStorageUuid(allocatedPrimaryStorageUuid);
                        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, allocatedPrimaryStorageUuid);
                        bus.send(msg);
                        chain.rollback();
                    }
                });

                flow(new Flow() {
                    String __name__ = "create-data-volume";

                    @Override
                    public void run(final FlowTrigger innerTrigger, Map data) {
                        String volumeFormat = VolumeFormat.getVolumeFormatByMasterHypervisorType(instantiateVm.getHypervisorType()).toString();

                        CreateVolumeMsg msg = new CreateVolumeMsg();
                        msg.setAccountUuid(accountUuid);
                        msg.setSize(diskSize);
                        msg.setName(diskAO.getName());
                        msg.setFormat(volumeFormat);
                        msg.setSystemTags(diskAO.getSystemTags());
                        msg.setVolumeType(VolumeType.Data.toString());
                        msg.setDiskOfferingUuid(diskAO.getDiskOfferingUuid());
                        msg.setPrimaryStorageUuid(allocatedPrimaryStorageUuid);
                        msg.setDescription(String.format("vm-%s-data-volume", vmUuid));
                        msg.setEncrypted(Boolean.TRUE.equals(diskAO.getEncrypted()));
                        bus.makeLocalServiceId(msg, VolumeConstant.SERVICE_ID);
                        bus.send(msg, new CloudBusCallBack(innerTrigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    innerTrigger.fail(reply.getError());
                                    return;
                                }
                                CreateVolumeReply cr = reply.castReply();
                                volumeInventory = cr.getInventory();
                                innerTrigger.next();
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback chain, Map data) {
                        if (volumeInventory == null) {
                            chain.rollback();
                            return;
                        }
                        DeleteVolumeMsg msg = new DeleteVolumeMsg();
                        msg.setUuid(volumeInventory.getUuid());
                        msg.setDeletionPolicy(VolumeDeletionPolicyManager.VolumeDeletionPolicy.Direct.toString());
                        bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, volumeInventory.getUuid());
                        bus.send(msg, new CloudBusCallBack(chain) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    chain.rollback();
                                    return;
                                }

                                DeleteVolumeGC gc = new DeleteVolumeGC();
                                gc.NAME = String.format("gc-volume-%s", volumeInventory.getUuid());
                                gc.deletionPolicy = VolumeDeletionPolicyManager.VolumeDeletionPolicy.Direct.toString();
                                gc.volumeUuid = volumeInventory.getUuid();
                                gc.submit(TimeUnit.HOURS.toSeconds(8), TimeUnit.SECONDS);

                                chain.rollback();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "instantiate-data-volume";

                    @Override
                    public void run(final FlowTrigger innerTrigger, Map data) {
                        InstantiateVolumeMsg imsg = new InstantiateVolumeMsg();
                        imsg.setHostUuid(hostUuid);
                        imsg.setSkipIfExisting(true);
                        imsg.setPrimaryStorageAllocated(true);
                        imsg.setSystemTags(diskAO.getSystemTags());
                        imsg.setVolumeUuid(volumeInventory.getUuid());
                        imsg.setAllocatedInstallUrl(allocatedInstallUrl);
                        imsg.setPrimaryStorageUuid(allocatedPrimaryStorageUuid);
                        bus.makeLocalServiceId(imsg, VolumeConstant.SERVICE_ID);
                        bus.send(imsg, new CloudBusCallBack(innerTrigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    innerTrigger.fail(reply.getError());
                                    return;
                                }
                                InstantiateVolumeReply cr = reply.castReply();
                                volumeInventory = cr.getVolume();
                                innerTrigger.next();
                            }
                        });
                    }
                });
            }

            private void setupVolumeFromTemplateUuidFlows() {
                final String[] allocatedPrimaryStorageUuid = new String[1];

                flow(new NoRollbackFlow() {
                    String __name__ = String.format("dryrun-allocate-primary-storage-for-templateUuid-%s", diskAO.getTemplateUuid());

                    @Override
                    public boolean skip(Map data) {
                        return diskAO.getPrimaryStorageUuid() != null;
                    }

                    @Override
                    public void run(final FlowTrigger innerTrigger, Map data) {
                        ImageVO template = dbf.findByUuid(diskAO.getTemplateUuid(), ImageVO.class);

                        AllocatePrimaryStorageSpaceMsg amsg = new AllocatePrimaryStorageSpaceMsg();
                        amsg.setDryRun(true);
                        amsg.setSize(template.getSize());
                        amsg.setPurpose(PrimaryStorageAllocationPurpose.CreateDataVolume.toString());
                        amsg.setRequiredHostUuid(instantiateVm.getLastHostUuid());
                        amsg.setRequiredClusterUuids(Collections.singletonList(instantiateVm.getClusterUuid()));
                        List<String> bsUuids = template.getBackupStorageRefs().stream()
                                .map(ImageBackupStorageRefVO::getBackupStorageUuid).collect(Collectors.toList());
                        amsg.setPossiblePrimaryStorageTypes(possiblePrimaryStorageTypes(bsUuids));
                        if (Boolean.TRUE.equals(diskAO.getEncrypted())) {
                            amsg.addRequiredFeature(PrimaryStorageFeature.ENCRYPTED_VOLUME);
                        }

                        bus.makeLocalServiceId(amsg, PrimaryStorageConstant.SERVICE_ID);
                        bus.send(amsg, new CloudBusCallBack(innerTrigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    innerTrigger.fail(reply.getError());
                                    return;
                                }
                                AllocatePrimaryStorageDryRunReply ar = (AllocatePrimaryStorageDryRunReply) reply;
                                allocatedPrimaryStorageUuid[0] = ar.getPrimaryStorageInventories().get(0).getUuid();
                                innerTrigger.next();
                            }
                        });
                    }

                    private List<String> possiblePrimaryStorageTypes(List<String> bsUuids) {
                        if (CollectionUtils.isEmpty(bsUuids)) {
                            return Collections.emptyList();
                        }
                        List<String> imageBsTypes = Q.New(BackupStorageVO.class).select(BackupStorageVO_.type)
                                .in(BackupStorageVO_.uuid, bsUuids).listValues();
                        List<String> possiblePrimaryStorageTypes = new ArrayList<>();
                        imageBsTypes.forEach(imageBsType -> possiblePrimaryStorageTypes
                                .addAll(hostAllocatorMgr.getBackupStoragePrimaryStorageMetrics().get(imageBsType)));
                        return possiblePrimaryStorageTypes.stream().distinct().collect(Collectors.toList());
                    }
                });

                flow(new Flow() {
                    String __name__ = String.format("instantiate-data-volume-from-template-%s", diskAO.getTemplateUuid());

                    @Override
                    public void run(final FlowTrigger innerTrigger, Map data) {
                        CreateDataVolumeFromVolumeTemplateMsg cmsg = new CreateDataVolumeFromVolumeTemplateMsg();
                        cmsg.setHostUuid(instantiateVm.getLastHostUuid());
                        cmsg.setImageUuid(diskAO.getTemplateUuid());
                        cmsg.setName(diskAO.getName());
                        cmsg.setAccountUuid(accountUuid);
                        cmsg.setSystemTags(new ArrayList<>());
                        if (!CollectionUtils.isEmpty(diskAO.getSystemTags())) {
                            cmsg.getSystemTags().addAll(diskAO.getSystemTags());
                        }
                        cmsg.setDescription(String.format("vm-%s-data-volume", vmUuid));
                        if (diskAO.getPrimaryStorageUuid() != null) {
                            cmsg.setPrimaryStorageUuid(diskAO.getPrimaryStorageUuid());
                        } else {
                            cmsg.setPrimaryStorageUuid(allocatedPrimaryStorageUuid[0]);
                        }
                        cmsg.setEncrypted(Boolean.TRUE.equals(diskAO.getEncrypted()));
                        cmsg.setEncryptedVolumeBackupRestore(diskAO.isVolumeBackupTemplate()
                                && Boolean.TRUE.equals(diskAO.getEncrypted()));

                        bus.makeLocalServiceId(cmsg, VolumeConstant.SERVICE_ID);
                        bus.send(cmsg, new CloudBusCallBack(innerTrigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    innerTrigger.fail(reply.getError());
                                    return;
                                }
                                VolumeInventory inv = ((CreateDataVolumeFromVolumeTemplateReply) reply).getInventory();
                                String volumeUuid = inv.getUuid();
                                VolumeVO vo = dbf.findByUuid(volumeUuid, VolumeVO.class);
                                vo.setVmInstanceUuid(vmUuid);
                                vo.setActualSize(vo.getActualSize() == null ? 0L : vo.getActualSize());
                                vo = dbf.updateAndRefresh(vo);
                                volumeInventory = VolumeInventory.valueOf(vo);
                                innerTrigger.next();
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback chain, Map data) {
                        if (volumeInventory == null) {
                            chain.rollback();
                            return;
                        }
                        DeleteVolumeMsg msg = new DeleteVolumeMsg();
                        msg.setDeletionPolicy(VolumeDeletionPolicyManager.VolumeDeletionPolicy.Direct.toString());
                        msg.setUuid(volumeInventory.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, volumeInventory.getUuid());
                        bus.send(msg, new CloudBusCallBack(chain) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    chain.rollback();
                                    return;
                                }

                                DeleteVolumeGC gc = new DeleteVolumeGC();
                                gc.NAME = String.format("gc-volume-%s", msg.getVolumeUuid());
                                gc.deletionPolicy = VolumeDeletionPolicyManager.VolumeDeletionPolicy.Direct.toString();
                                gc.volumeUuid = msg.getVolumeUuid();
                                gc.submit(TimeUnit.HOURS.toSeconds(8), TimeUnit.SECONDS);

                                chain.rollback();
                            }
                        });
                    }
                });
            }

            private void setupAttachVolumeFlows() {
                flow(new NoRollbackFlow() {
                    String __name__ = String.format("attach-volume-to-vm-%s", vmUuid);

                    @Override
                    public void run(final FlowTrigger innerTrigger, Map data) {
                        AttachDataVolumeToVmMsg amsg = new AttachDataVolumeToVmMsg();
                        amsg.setVmInstanceUuid(vmUuid);
                        amsg.setVolume(volumeInventory);
                        bus.makeTargetServiceIdByResourceUuid(amsg, VmInstanceConstant.SERVICE_ID, vmUuid);
                        bus.send(amsg, new CloudBusCallBack(innerTrigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    innerTrigger.fail(reply.getError());
                                    return;
                                }
                                innerTrigger.next();
                            }
                        });
                    }
                });
            }

            private void setupAttachOtherDiskFlows() {
                flow(new NoRollbackFlow() {
                    String __name__ = String.format("attach-other-Disk-to-vm-%s", vmUuid);

                    @Override
                    public void run(final FlowTrigger innerTrigger, Map data) {
                        VmAttachOtherDiskExtensionPoint vmAttachOtherDiskExtensionPoint = pluginRgty
                                .getExtensionFromMap(diskAO.getSourceType(), VmAttachOtherDiskExtensionPoint.class);
                        if (vmAttachOtherDiskExtensionPoint == null) {
                            innerTrigger.fail(operr("the disk does not support attachment. disk type is %s", diskAO.getSourceType()));
                            return;
                        }
                        vmAttachOtherDiskExtensionPoint.attachOtherDiskToVm(diskAO, vmUuid, new Completion(innerTrigger) {
                            @Override
                            public void success() {
                                innerTrigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                innerTrigger.fail(errorCode);
                            }
                        });
                    }
                });
            }
        }).start();
    }

    @Override
    public void rollback(FlowRollback chain, Map data) {
        if (volumeInventory == null) {
            chain.rollback();
            return;
        }

        DeleteVolumeMsg msg = new DeleteVolumeMsg();
        msg.setDeletionPolicy(VolumeDeletionPolicyManager.VolumeDeletionPolicy.Direct.toString());
        msg.setUuid(volumeInventory.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, volumeInventory.getUuid());
        bus.send(msg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    chain.rollback();
                    return;
                }
                DeleteVolumeGC gc = new DeleteVolumeGC();
                gc.NAME = String.format("gc-volume-%s", msg.getVolumeUuid());
                gc.deletionPolicy = VolumeDeletionPolicyManager.VolumeDeletionPolicy.Direct.toString();
                gc.volumeUuid = msg.getVolumeUuid();
                gc.submit(TimeUnit.HOURS.toSeconds(8), TimeUnit.SECONDS);
                chain.rollback();
            }
        });
    }
}
