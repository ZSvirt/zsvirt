package org.zstack.appliancevm;

import org.zstack.header.core.workflow.Flow;
import org.zstack.kvm.KVMConstant;

/**
 */
public class ApplianceVmKvmBootstrapFlowFactory implements ApplianceVmBootstrapFlowFactory {
    @Override
    public String getHypervisorTypeForApplianceVmBootstrapFlow() {
        return KVMConstant.KVM_HYPERVISOR_TYPE;
    }

    @Override
    public Flow createApplianceVmBootstrapInfoFlow() {
        return new ApplianceVmKvmBootstrapFlow();
    }
}
