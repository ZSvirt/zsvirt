package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.allocator.HostAllocatorManager;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.BackupStorageVO_;
import org.zstack.header.storage.primary.AllocatePrimaryStorageSpaceMsg;
import org.zstack.header.storage.primary.AllocatePrimaryStorageSpaceReply;
import org.zstack.header.storage.primary.PrimaryStorageAllocationPurpose;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageFeature;
import org.zstack.header.storage.primary.ReleasePrimaryStorageSpaceMsg;
import org.zstack.header.vm.DiskAO;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceSpec.VolumeSpec;
import org.zstack.header.volume.VolumeType;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.zstack.core.Platform.err;
import static org.zstack.header.image.ImageConstant.SNAPSHOT_REUSE_IMAGE_SCHEMA;
import static org.zstack.header.vm.VmErrors.WRONG_SPECIFIC_PS_ERROR;
import static org.zstack.utils.CollectionUtils.isEmpty;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmAllocatePrimaryStorageFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(VmAllocatePrimaryStorageFlow.class);
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected HostAllocatorManager hostAllocatorMgr;

    @Override
    public void run(final FlowTrigger trigger, final Map data) {
        final List<AllocatePrimaryStorageSpaceMsg> msgs = new ArrayList<>();
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        HostInventory destHost = spec.getDestHost();

        msgs.add(buildMessageForRootVolume(spec, destHost));
        msgs.addAll(buildMessageForDataVolumes(spec, destHost));

        new While<>(msgs).each((msg, whileCompletion) -> {
            bus.send(msg, new CloudBusCallBack(whileCompletion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        whileCompletion.addError(reply.getError());
                        whileCompletion.allDone();
                        return;
                    }

                    VolumeSpec volumeSpec = new VolumeSpec();
                    AllocatePrimaryStorageSpaceReply ar = (AllocatePrimaryStorageSpaceReply) reply;
                    volumeSpec.setAllocatedInstallUrl(ar.getAllocatedInstallUrl());
                    volumeSpec.setPrimaryStorageInventory(ar.getPrimaryStorageInventory());
                    volumeSpec.setSize(ar.getSize());
                    volumeSpec.setType(PrimaryStorageAllocationPurpose.CreateNewVm.toString().equals(msg.getPurpose()) ?
                            VolumeType.Root.toString() : VolumeType.Data.toString());
                    volumeSpec.setDiskOfferingUuid(msg.getDiskOfferingUuid());
                    volumeSpec.setTags(msg.getSystemTags());
                    if (VolumeType.Root.toString().equals(volumeSpec.getType())) {
                        spec.getRootDisk().setPrimaryStorageUuid(ar.getPrimaryStorageInventory().getUuid());
                    } else {
                        spec.setAllocatedPrimaryStorageUuidForDataVolume(ar.getPrimaryStorageInventory().getUuid());
                    }

                    spec.getVolumeSpecs().add(volumeSpec);
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

    private AllocatePrimaryStorageSpaceMsg buildMessageForRootVolume(final VmInstanceSpec spec, HostInventory destHost) {
        AllocatePrimaryStorageSpaceMsg rmsg = new AllocatePrimaryStorageSpaceMsg();

        DiskAO disk = spec.getRootDisk();

        rmsg.setVmInstanceUuid(spec.getVmInventory().getUuid());
        if (spec.getImageSpec() != null) {
            if (spec.getImageSpec().getInventory() != null) {
                rmsg.setImageUuid(spec.getImageSpec().getInventory().getUuid());
                if (spec.getImageSpec().getInventory().getUrl().startsWith(SNAPSHOT_REUSE_IMAGE_SCHEMA)) {
                    rmsg.setRequiredInstallUri(spec.getImageSpec().getInventory().getUrl());
                }
            }
            Optional.ofNullable(spec.getImageSpec().getSelectedBackupStorage())
                    .ifPresent(it -> rmsg.setBackupStorageUuid(it.getBackupStorageUuid()));
        }
        rmsg.setSize(disk != null && disk.getSize() > 0 ? disk.getSize() : spec.getRootDiskAllocateSize());
        if (spec.getRootDiskOffering() != null) {
            rmsg.setDiskOfferingUuid(spec.getRootDiskOffering().getUuid());
            rmsg.setAllocationStrategy(spec.getRootDiskOffering().getAllocatorStrategy());
        }

        rmsg.setRequiredHostUuid(destHost.getUuid());
        rmsg.setPurpose(PrimaryStorageAllocationPurpose.CreateNewVm.toString());

        final List<String> candidatePs = spec.getCandidatePrimaryStorageUuidsForRootVolume();
        if (disk != null && disk.getPrimaryStorageUuid() != null) {
            if (!isEmpty(candidatePs) && !candidatePs.contains(disk.getPrimaryStorageUuid())) {
                throw new OperationFailureException(err(WRONG_SPECIFIC_PS_ERROR,
                        "failed to allocate root volume to the primary storage[%s]", disk.getPrimaryStorageUuid())
                        .withOpaque("required.primary.storage.uuid", disk.getPrimaryStorageUuid())
                        .withOpaque("candidate.primary.storage.uuid.list", candidatePs));
            }
            rmsg.setRequiredPrimaryStorageUuid(disk.getPrimaryStorageUuid());
        } else {
            rmsg.setCandidatePrimaryStorageUuids(candidatePs);
            rmsg.setPossiblePrimaryStorageTypes(selectPsTypesFromSpec(spec));
        }
        if (disk != null && Boolean.TRUE.equals(disk.getEncrypted())
                && disk.getPrimaryStorageUuid() == null && isEmpty(candidatePs)) {
            rmsg.addRequiredFeature(PrimaryStorageFeature.ENCRYPTED_VOLUME);
        }

        Set<String> tags = new HashSet<>();
        if (disk != null && disk.getSystemTags() != null) {
            tags.addAll(disk.getSystemTags());
        }
        if (spec.getRootVolumeSystemTags() != null) {
            tags.addAll(spec.getRootVolumeSystemTags());
        }

        rmsg.setSystemTags(new ArrayList<>(tags));
        bus.makeLocalServiceId(rmsg, PrimaryStorageConstant.SERVICE_ID);
        return rmsg;
    }

    private List<AllocatePrimaryStorageSpaceMsg> buildMessageForDataVolumes(final VmInstanceSpec spec, HostInventory destHost) {
        List<DiskAO> nonTemplateDeprecatedDisks = spec.getNonTemplateDeprecatedDisksSpecs();
        int dataVolumeCount = isEmpty(nonTemplateDeprecatedDisks) ? 0 : nonTemplateDeprecatedDisks.size();

        if (dataVolumeCount == 0) {
            return Collections.emptyList();
        }

        List<AllocatePrimaryStorageSpaceMsg> msgs = new ArrayList<>();
        for (int i = 0; i < dataVolumeCount; i++) {
            DiskAO deprecatedDisk = nonTemplateDeprecatedDisks.get(i);

            AllocatePrimaryStorageSpaceMsg amsg = new AllocatePrimaryStorageSpaceMsg();
            amsg.setCandidatePrimaryStorageUuids(spec.getCandidatePrimaryStorageUuidsForDataVolume());
            amsg.setSize(deprecatedDisk != null && deprecatedDisk.getSize() > 0 ? deprecatedDisk.getSize() : 0);
            amsg.setRequiredHostUuid(destHost.getUuid());
            amsg.setAllocationStrategy(PrimaryStorageConstant.DEFAULT_PRIMARY_STORAGE_ALLOCATION_STRATEGY_TYPE);
            amsg.setPurpose(PrimaryStorageAllocationPurpose.CreateDataVolume.toString());
            amsg.setDiskOfferingUuid(deprecatedDisk != null ? deprecatedDisk.getDiskOfferingUuid() : null);

            Set<String> tags = new HashSet<>();
            if (spec.getDataVolumeSystemTags() != null) {
                tags.addAll(spec.getDataVolumeSystemTags());
            }
            if (deprecatedDisk != null && !isEmpty(deprecatedDisk.getSystemTags())) {
                tags.addAll(deprecatedDisk.getSystemTags());
            }

            amsg.setSystemTags(new ArrayList<>(tags));
            bus.makeLocalServiceId(amsg, PrimaryStorageConstant.SERVICE_ID);
            msgs.add(amsg);
        }

        return msgs;
    }

    @Override
    public void rollback(FlowRollback chain, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        for (VolumeSpec vspec : spec.getVolumeSpecs()) {
            if (vspec.isVolumeCreated()) {
                // don't return capacity as it has been returned when the volume is deleted
                continue;
            }

            ReleasePrimaryStorageSpaceMsg msg = new ReleasePrimaryStorageSpaceMsg();
            msg.setAllocatedInstallUrl(vspec.getAllocatedInstallUrl());
            msg.setDiskSize(vspec.getSize());
            msg.setPrimaryStorageUuid(vspec.getPrimaryStorageInventory().getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, vspec.getPrimaryStorageInventory().getUuid());
            bus.send(msg);
        }

        spec.getVolumeSpecs().clear();
        chain.rollback();
    }

    private List<String> selectPsTypesFromSpec(final VmInstanceSpec spec) {
        // get ps types from image bs and cdroms bs
        List<String> psTypes = null;
        if (spec.getImageSpec().isNeedDownload() || spec.getImageSpec().getSelectedBackupStorage() != null) {
            String imageBsType = findImageBsType(spec.getImageSpec().getSelectedBackupStorage().getBackupStorageUuid());
            psTypes = hostAllocatorMgr.getBackupStoragePrimaryStorageMetrics().get(imageBsType);
            DebugUtils.Assert(psTypes != null, "why primaryStorageTypes is null");
        }

        for (VmInstanceSpec.CdRomSpec cdRom : spec.getCdRomSpecs()) {
            if (!cdRom.isAttachedIso()) {
                continue;
            }

            String cdRomBsType = findImageBsType(cdRom.getBackupStorageUuid());
            if (psTypes == null) {
                psTypes = hostAllocatorMgr.getBackupStoragePrimaryStorageMetrics().get(cdRomBsType);
            } else {
                psTypes = new ArrayList<>(psTypes);
                psTypes.retainAll(hostAllocatorMgr.getBackupStoragePrimaryStorageMetrics().get(cdRomBsType));
            }
        }
        return psTypes;
    }

    private String findImageBsType(String bsUuid) {
        return Q.New(BackupStorageVO.class)
                .select(BackupStorageVO_.type)
                .eq(BackupStorageVO_.uuid, bsUuid)
                .findValue();
    }
}
