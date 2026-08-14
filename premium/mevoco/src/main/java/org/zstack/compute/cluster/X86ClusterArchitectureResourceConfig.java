package org.zstack.compute.cluster;

import org.zstack.compute.cluster.arch.ClusterArchitectureResourceConfig;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.header.host.CpuArchitecture;
import org.zstack.resourceconfig.ResourceConfigValidatorExtensionPoint;

import java.util.*;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/1/27 09:48
 */
public class X86ClusterArchitectureResourceConfig extends ClusterArchitectureResourceConfig {
    public X86ClusterArchitectureResourceConfig() {
        registerDefaultValue(MevocoClusterGlobalConfig.HUGEPAGE_SIZE.getIdentity(), "2");
        registerDefaultValue(VmGlobalConfig.VM_MAX_VCPU.getIdentity(), "128");
    }

    @Override
    public String getArchitecture() {
        return CpuArchitecture.x86_64.name();
    }
}
