package org.zstack.vpc.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.ApplianceVmType;
import org.zstack.header.host.CpuArchitecture;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.network.service.virtualrouter.vyos.VyosKvmVmBaseFactory;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.vpc.VpcGlobalConfig;
import org.zstack.vpc.VpcVRouterFactory;

public class VpcVRouterKvmFactory extends VyosKvmVmBaseFactory {
    @Autowired
    private ResourceConfigFacade rcf;

    @Override
    public ApplianceVmType getApplianceVmType() {
        return VpcVRouterFactory.applianceVmType;
    }

    @Override
    public void createHypervisorBasedConfigurations(VmInstanceSpec spec) {
        super.createHypervisorBasedConfigurations(spec);

        String architecture = spec.getDestHost().getArchitecture();

        if (!CpuArchitecture.x86_64.toString().equals(architecture)) {
            // set VPC router's CONFIG_FIREWALL_WITH_IPTABLES to default TRUE
            ResourceConfig rc = rcf.getResourceConfig(VpcGlobalConfig.CONFIG_FIREWALL_WITH_IPTABLES.getIdentity());
            rc.updateValue(spec.getVmInventory().getUuid(), Boolean.TRUE.toString());
        }
    }
}
