package org.zstack.appliancevm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.VmPriorityGlobalProperty;
import org.zstack.compute.vm.VmPriorityOperator;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.*;
import org.zstack.core.db.Q;
import org.zstack.header.Component;
import org.zstack.header.core.NopeNoErrorCompletion;
import org.zstack.header.core.NopeWhileDoneCompletion;
import org.zstack.header.core.WhileCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostCanonicalEvents;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostStatus;
import org.zstack.header.message.MessageReply;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vm.*;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.kvm.KVMStartVmExtensionPoint;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ApplianceVmPriorityUpgradeConfigExtensionPoint implements Component, KVMStartVmExtensionPoint, UpdatePriorityConfigExtensionPoint {
    private static final CLogger logger = Utils.getLogger(ApplianceVmPriorityUpgradeConfigExtensionPoint.class);

    @Autowired
    private EventFacade evtf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ResourceDestinationMaker destinationMaker;
    @Autowired
    private VmPriorityOperator priorityOperator;

    @Override
    public boolean start() {
        initRunningApplianceVmPriority();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private void updateApplianceVmPriorityOnHost(HostInventory inv) {
        List<String> vmUuids = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.uuid)
                .eq(VmInstanceVO_.hostUuid, inv.getUuid())
                .eq(VmInstanceVO_.type, ApplianceVmConstant.APPLIANCE_VM_TYPE)
                .eq(VmInstanceVO_.state, VmInstanceState.Running)
                .listValues();

        if (vmUuids.isEmpty()) {
            return;
        }

        //Avoid appliancevm repeat setting priority
        List<String> updatedVms = Q.New(SystemTagVO.class)
                .select(SystemTagVO_.resourceUuid)
                .like(SystemTagVO_.tag, "vmPriority::ApplianceVmHigh")
                .in(SystemTagVO_.resourceUuid, vmUuids)
                .listValues();

        vmUuids.removeAll(updatedVms);
        vmUuids.removeIf(v -> !destinationMaker.isManagedByUs(v));

        if (vmUuids.isEmpty()) {
            return;
        }

        VmPriorityConfigVO priorityVO = Q.New(VmPriorityConfigVO.class).eq(VmPriorityConfigVO_.level, VmPriorityLevel.ApplianceVmHigh).find();
        List<PriorityConfigStruct> priorityConfigStructs = new ArrayList<>();
        vmUuids.forEach(v -> {
            priorityConfigStructs.add(new PriorityConfigStruct(priorityVO, v));
        });

        UpdateVmPriorityMsg msg = new UpdateVmPriorityMsg();
        msg.setHostUuid(inv.getUuid());
        msg.setPriorityConfigStructs(priorityConfigStructs);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, inv.getUuid());
        bus.send(msg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("update vms priority failed on host[%s],because %s",
                            inv.getUuid(), reply.getError()));
                    return;
                }
                new VmPriorityOperator().batchSetVmPriority(vmUuids, VmPriorityLevel.ApplianceVmHigh);
            }
        });
    }

    private void initRunningApplianceVmPriority() {
        if (!VmPriorityGlobalProperty.initRunningApplianceVmPriority) {
            return;
        }

        evtf.on(HostCanonicalEvents.HOST_STATUS_CHANGED_PATH, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                HostCanonicalEvents.HostStatusChangedData d = (HostCanonicalEvents.HostStatusChangedData) data;
                if (!d.getNewStatus().equals(HostStatus.Connected.toString())
                        || !d.getInventory().getHypervisorType().equals("KVM")) {
                    return;
                }
                updateApplianceVmPriorityOnHost(d.getInventory());
            }
        });
    }

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        if (!spec.getVmInventory().getType().equals(ApplianceVmConstant.APPLIANCE_VM_TYPE)) {
            return;
        }
        if (priorityOperator.getVmPriority(spec.getVmInventory().getUuid()).equals(VmPriorityLevel.ApplianceVmHigh)) {
            return;
        }
        priorityOperator.setVmPriority(spec.getVmInventory().getUuid(), VmPriorityLevel.ApplianceVmHigh);
        VmPriorityConfigVO priorityVO = Q.New(VmPriorityConfigVO.class).eq(VmPriorityConfigVO_.level, VmPriorityLevel.ApplianceVmHigh).find();
        cmd.setPriorityConfigStruct(new PriorityConfigStruct(priorityVO, spec.getVmInventory().getUuid()));
    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {

    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {

    }

    @Override
    public void afterUpdatePriorityConfig(VmPriorityConfigVO vmPriorityConfigVO) {
        if (!vmPriorityConfigVO.getLevel().equals(VmPriorityLevel.ApplianceVmHigh)) {
            return;
        }
        List<VmInstanceVO> vms = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.type, ApplianceVmConstant.APPLIANCE_VM_TYPE)
                .eq(VmInstanceVO_.state, VmInstanceState.Running)
                .list();
        vms.removeIf(v -> !destinationMaker.isManagedByUs(v.getUuid()));
        if (vms.isEmpty()) {
            return;
        }
        new While<>(vms).all((VmInstanceVO vm, WhileCompletion com) -> {
            UpdateVmPriorityMsg msg = new UpdateVmPriorityMsg();
            msg.setHostUuid(vm.getHostUuid());
            msg.setPriorityConfigStructs(Arrays.asList(new PriorityConfigStruct(vmPriorityConfigVO, vm.getUuid())));
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, vm.getHostUuid());
            bus.send(msg, new CloudBusCallBack(msg) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(String.format("update ApplianceVm[%s] priority to [%s] failed, because %s",
                                vm.getUuid(), VmPriorityLevel.ApplianceVmHigh, reply.getError()));
                    }
                    com.done();
                }
            });
        }).run(new NopeWhileDoneCompletion());
    }
}
