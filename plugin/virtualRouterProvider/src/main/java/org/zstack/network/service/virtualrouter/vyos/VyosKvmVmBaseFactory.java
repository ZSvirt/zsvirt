package org.zstack.network.service.virtualrouter.vyos;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.ApplianceVmType;
import org.zstack.appliancevm.kvm.KvmApplianceVmSubTypeFactory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMGlobalConfig;
import org.zstack.network.service.virtualrouter.VirtualRouterApplianceVmFactory;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;

public class VyosKvmVmBaseFactory implements KvmApplianceVmSubTypeFactory {
    @Autowired
    private ResourceConfigFacade rcf;

    @Override
    public ApplianceVmType getApplianceVmType() {
        return VirtualRouterApplianceVmFactory.type;
    }

    @Override
    public void createHypervisorBasedConfigurations(VmInstanceSpec spec) {
        // set VPC router's CPU mode to default NONE
        ResourceConfig rc = rcf.getResourceConfig(KVMGlobalConfig.NESTED_VIRTUALIZATION.getIdentity());
        rc.updateValue(spec.getVmInventory().getUuid(), KVMConstant.CPU_MODE_NONE);

        // set VPC log to File
        rc = rcf.getResourceConfig(KVMGlobalConfig.REDIRECT_CONSOLE_LOG_TO_FILE.getIdentity());
        rc.updateValue(spec.getVmInventory().getUuid(), Boolean.TRUE.toString());
    }
}
