package org.zstack.kvm.hypervisor;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.GetVirtualizerInfoMsg;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.*;
import org.zstack.kvm.*;
import org.zstack.kvm.KVMAgentCommands.DestroyVmCmd;
import org.zstack.kvm.KVMAgentCommands.RebootVmResponse;
import org.zstack.kvm.KVMAgentCommands.StopVmCmd;
import org.zstack.kvm.KVMAgentCommands.VmDevicesInfoResponse;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.zstack.core.Platform.operr;
import static org.zstack.kvm.KVMAgentCommands.*;

/**
 * Created by Wenhao.Zhang on 23/02/27
 */
public class KvmHypervisorInfoExtensions implements
        KVMSyncVmDeviceInfoExtensionPoint,
        KVMRebootVmExtensionPoint,
        KVMDestroyVmExtensionPoint,
        KVMStopVmExtensionPoint,
        VmAfterExpungeExtensionPoint,
        KVMHostConnectExtensionPoint,
        VmInstanceMigrateExtensionPoint
{
    private static final CLogger logger = Utils.getLogger(KvmHypervisorInfoExtensions.class);

    @Autowired
    private KvmHypervisorInfoManager manager;
    @Autowired
    private CloudBus bus;

    @Override
    public void afterReceiveVmDeviceInfoResponse(VmInstanceInventory vm, VmDevicesInfoResponse rsp, VmInstanceSpec spec) {
        Optional.ofNullable(rsp.getVirtualizerInfo()).ifPresent(manager::saveVmInfo);
    }

    @Override
    public void beforeRebootVmOnKvm(KVMHostInventory host, VmInstanceInventory vm) throws KVMException {}
    @Override
    public void rebootVmOnKvmFailed(KVMHostInventory host, VmInstanceInventory vm, ErrorCode err) {}

    @Override
    public void rebootVmOnKvmSuccess(KVMHostInventory host, VmInstanceInventory vm, RebootVmResponse rsp) {
        Optional.ofNullable(rsp.getVirtualizerInfo()).ifPresent(manager::saveVmInfo);
    }

    @Override
    public void beforeDestroyVmOnKvm(KVMHostInventory host, VmInstanceInventory vm, DestroyVmCmd cmd)
            throws KVMException {}

    @Override
    public void beforeDirectlyDestroyVmOnKvm(DestroyVmCmd cmd) {}

    @Override
    public void destroyVmOnKvmSuccess(KVMHostInventory host, VmInstanceInventory vm) {
        manager.clean(vm.getUuid());
    }

    @Override
    public void destroyVmOnKvmFailed(KVMHostInventory host, VmInstanceInventory vm, ErrorCode err) {}

    @Override
    public void beforeStopVmOnKvm(KVMHostInventory host, VmInstanceInventory vm, StopVmCmd cmd) throws KVMException {}

    @Override
    public void stopVmOnKvmSuccess(KVMHostInventory host, VmInstanceInventory vm) {
        manager.clean(vm.getUuid());
    }

    @Override
    public void stopVmOnKvmFailed(KVMHostInventory host, VmInstanceInventory vm, ErrorCode err) {}

    @Override
    public void vmAfterExpunge(VmInstanceInventory vm) {
        manager.clean(vm.getUuid());
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Flow createKvmHostConnectingFlow(KVMHostConnectedContext context) {
        final String hostUuid = context.getInventory().getUuid();
        final boolean newAdd = context.isNewAddedHost();

        return new NoRollbackFlow() {
            String __name__ = "collect-virtualizer-info-for-running-vm";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                GetVirtualizerInfoMsg msg = new GetVirtualizerInfoMsg();
                msg.setHostUuid(hostUuid);
                if (newAdd) {
                    msg.setVmInstanceUuids(Collections.emptyList());
                } else {
                    List<String> vmUuids = Q.New(VmInstanceVO.class)
                            .eq(VmInstanceVO_.hostUuid, hostUuid)
                            .select(VmInstanceVO_.uuid)
                            .listValues();
                    msg.setVmInstanceUuids(vmUuids);
                }

                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            trigger.next();
                        } else {
                            trigger.fail(operr("failed to collect host virtualizer info").withCause(reply.getError()));
                        }
                    }
                });
            }
        };
    }

    @Override
    public void afterMigrateVm(VmInstanceInventory inv, String srcHostUuid) {
        final String hostUuid = inv.getHostUuid();
        if (hostUuid == null) {
            return;
        }

        GetVirtualizerInfoMsg msg = new GetVirtualizerInfoMsg();
        msg.setHostUuid(hostUuid);
        msg.setVmInstanceUuids(Collections.singletonList(inv.getUuid()));
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to get virtualizer info for VM[uuid:%s]: %s",
                            inv.getUuid(), reply.getError()));
                }
            }
        });
    }

}
