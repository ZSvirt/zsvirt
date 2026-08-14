package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.vm.VmBeforeStartOnHypervisorExtensionPoint;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;

import static org.zstack.core.Platform.operr;
import static org.zstack.kvm.KVMConstant.CPU_MODE_NONE;

public class KvmVmHardwareVerifyExtensionPoint implements VmBeforeStartOnHypervisorExtensionPoint {
    @Autowired
    private ResourceConfigFacade rcf;

    @Override
    public void beforeStartVmOnHypervisor(VmInstanceSpec spec) {
        if (!spec.getVmInventory().getHypervisorType().equals(KVMConstant.KVM_HYPERVISOR_TYPE)) {
            return;
        }

        ResourceConfig resourceConfig = rcf.getResourceConfig(KVMGlobalConfig.VM_CPU_HYPERVISOR_FEATURE.getIdentity());
        Boolean enableHypervisor = resourceConfig.getResourceConfigValue(spec.getVmInventory().getUuid(), Boolean.class);
        if (enableHypervisor) {
            return;
        }

        ResourceConfig cpuMode = rcf.getResourceConfig(KVMGlobalConfig.NESTED_VIRTUALIZATION.getIdentity());
        if (CPU_MODE_NONE.equals(cpuMode.getResourceConfigValue(spec.getVmInventory().getUuid(), String.class))) {
            throw new OperationFailureException(operr("Failed to start vm," +
                    " because can not disable vm.cpu.hypervisor.feature with vm.cpuMode none"));
        }
    }
}
