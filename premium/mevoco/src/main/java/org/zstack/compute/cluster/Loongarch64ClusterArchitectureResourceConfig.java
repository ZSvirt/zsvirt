package org.zstack.compute.cluster;

import org.zstack.compute.cluster.arch.ClusterArchitectureResourceConfig;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.header.host.CpuArchitecture;
import org.zstack.resourceconfig.ResourceConfigValidatorExtensionPoint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/4/1 14:02
 */
public class Loongarch64ClusterArchitectureResourceConfig extends ClusterArchitectureResourceConfig {
    public Loongarch64ClusterArchitectureResourceConfig() {
        registerDefaultValue(MevocoClusterGlobalConfig.HUGEPAGE_SIZE.getIdentity(), "32");
        registerDefaultValue(VmGlobalConfig.VM_MAX_VCPU.getIdentity(), "32");
    }

    @Override
    public String getArchitecture() {
        return CpuArchitecture.loongarch64.name();
    }
}
