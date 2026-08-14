package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.allocator.HostAllocatorManager;
import org.zstack.core.asyncbatch.AsyncLoop;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.BackupStorageVO_;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceSpec.VolumeSpec;
import org.zstack.header.volume.VolumeType;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.*;

import static org.zstack.utils.CollectionUtils.isEmpty;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ApplianceVmAllocatePrimaryStorageFlow implements Flow {
    protected static final CLogger logger = Utils.getLogger(ApplianceVmAllocatePrimaryStorageFlow.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected HostAllocatorManager hostAllocatorMgr;
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public void run(final FlowTrigger trigger, final Map data) {
        final List<AllocatePrimaryStorageSpaceMsg> msgs = new ArrayList<>();
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        HostInventory destHost = spec.getDestHost();
        final ImageInventory iminv = spec.getImageSpec().getInventory();

        // get ps types from bs
        String bsType = Q.New(BackupStorageVO.class)
                .select(BackupStorageVO_.type)
                .eq(BackupStorageVO_.uuid, spec.getImageSpec().getSelectedBackupStorage().getBackupStorageUuid())
                .findValue();
        List<String> primaryStorageTypes = hostAllocatorMgr.getBackupStoragePrimaryStorageMetrics().get(bsType);
        DebugUtils.Assert(primaryStorageTypes != null, "why primaryStorageTypes is null");

        DebugUtils.Assert(isEmpty(spec.getDeprecatedDisksSpecs()), "create appliance vm can not with data volume");

        for (PrimaryStorageAllocatorStrategyExtensionPoint ext : pluginRgty.getExtensionList(PrimaryStorageAllocatorStrategyExtensionPoint.class)) {
            String allocatorStrategyType = ext.getAllocatorStrategy(destHost);
            if (allocatorStrategyType == null) {
                continue;
            }

            AllocatePrimaryStorageSpaceMsg rmsg = new AllocatePrimaryStorageSpaceMsg();
            rmsg.setCandidatePrimaryStorageUuids(spec.getCandidatePrimaryStorageUuidsForRootVolume());
            rmsg.setVmInstanceUuid(spec.getVmInventory().getUuid());
            if (spec.getImageSpec() != null) {
                rmsg.setImageUuid(spec.getImageSpec().getInventory().getUuid());
                Optional.ofNullable(spec.getImageSpec().getSelectedBackupStorage())
                        .ifPresent(it -> rmsg.setBackupStorageUuid(it.getBackupStorageUuid()));
            }
            rmsg.setSize(iminv.getSize());
            rmsg.setRequiredHostUuid(destHost.getUuid());
            rmsg.setPurpose(PrimaryStorageAllocationPurpose.CreateNewVm.toString());
            rmsg.setPossiblePrimaryStorageTypes(primaryStorageTypes);
            rmsg.setAllocationStrategy(allocatorStrategyType);
            rmsg.setSystemTags(spec.getRootVolumeSystemTags());

            bus.makeLocalServiceId(rmsg, PrimaryStorageConstant.SERVICE_ID);
            msgs.add(rmsg);
        }

        new AsyncLoop<AllocatePrimaryStorageSpaceMsg>(trigger) {
            boolean isAllocateSuccess = false;

            @Override
            protected Collection<AllocatePrimaryStorageSpaceMsg> collectionForLoop() {
                return msgs;
            }

            @Override
            protected void run(AllocatePrimaryStorageSpaceMsg msg, Completion completion) {
                if(isAllocateSuccess){
                    completion.success();
                    return;
                }

                bus.send(msg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {

                        if (!reply.isSuccess() && (msgs.indexOf(msg) + 1 < msgs.size())) {
                            // After failing, continue until all operations fail
                            logger.warn(String.format("Try to allocate failed, allocate primary storage msg: %s", JSONObjectUtil.toJsonString(msg)));
                            completion.success();
                            return;
                        }else if(!reply.isSuccess()){
                            completion.fail(reply.getError());
                            return;
                        }

                        isAllocateSuccess = true;

                        VolumeSpec volumeSpec = new VolumeSpec();
                        AllocatePrimaryStorageSpaceReply ar = (AllocatePrimaryStorageSpaceReply) reply;
                        volumeSpec.setAllocatedInstallUrl(ar.getAllocatedInstallUrl());
                        volumeSpec.setPrimaryStorageInventory(ar.getPrimaryStorageInventory());
                        volumeSpec.setSize(ar.getSize());
                        volumeSpec.setType(msg.getImageUuid() != null ? VolumeType.Root.toString() : VolumeType.Data.toString());
                        volumeSpec.setTags(spec.getRootVolumeSystemTags());
                        spec.getVolumeSpecs().add(volumeSpec);

                        completion.success();
                    }
                });
            }

            @Override
            protected void done() {
                trigger.next();
            }

            @Override
            protected void error(ErrorCode errorCode) {
                trigger.fail(errorCode);
            }
        }.start();
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
}
