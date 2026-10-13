package org.zstack.kvm;

import org.zstack.header.core.workflow.Flow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.kvm.KVMAgentCommands.StartVmCmd;

public interface KVMStartVmExtensionPoint {
    default void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, StartVmCmd cmd) {}

    default Flow beforeStartVmOnKvmFlow() {
        return Flow.of(getClass().getSimpleName() + "-before-start-vm-on-kvm-flow")
                .handle(((trigger, data) -> {
                    KVMHostInventory host = (KVMHostInventory) data.get(Param.HOST);
                    VmInstanceSpec spec = (VmInstanceSpec) data.get(Param.SPEC);
                    StartVmCmd cmd = (StartVmCmd) data.get(Param.CMD);
                    beforeStartVmOnKvm(host, spec, cmd);
                    trigger.next();
                }))
                .rollback((trigger, data) -> {
                    KVMHostInventory host = (KVMHostInventory) data.get(Param.HOST);
                    VmInstanceSpec spec = (VmInstanceSpec) data.get(Param.SPEC);
                    startVmOnKvmFailed(host, spec, trigger.getErrorCode());
                    trigger.rollback();
                })
                .build();
    }

    void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec);

    default void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {}

    class Param {
        public static final String HOST = "HOST";
        public static final String SPEC = "SPEC";
        public static final String CMD = "CMD";
        public static final String RESPONSE = "RESPONSE";
    }
}
