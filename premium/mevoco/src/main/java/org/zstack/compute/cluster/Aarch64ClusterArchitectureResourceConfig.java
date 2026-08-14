package org.zstack.compute.cluster;

import org.zstack.compute.cluster.arch.ClusterArchitectureResourceConfig;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.header.host.CpuArchitecture;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMGlobalConfig;
import org.zstack.resourceconfig.ResourceConfigValidatorExtensionPoint;

import java.util.*;

import static org.zstack.utils.CollectionDSL.list;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/1/27 09:48
 */
public class Aarch64ClusterArchitectureResourceConfig extends ClusterArchitectureResourceConfig {
    public Aarch64ClusterArchitectureResourceConfig() {
        registerDefaultValue(MevocoClusterGlobalConfig.HUGEPAGE_SIZE.getIdentity(), "2");
        registerDefaultValue(KVMGlobalConfig.NESTED_VIRTUALIZATION.getIdentity(), KVMConstant.CPU_MODE_HOST_PASSTHROUGH);
        registerDefaultValue(VmGlobalConfig.VM_MAX_VCPU.getIdentity(), "128");

        registerValidaValues(VmGlobalConfig.VM_VIDEO_TYPE.getIdentity(), list("virtio"));
        registerValidaValues(KVMGlobalConfig.NESTED_VIRTUALIZATION.getIdentity(),
                list(KVMConstant.CPU_MODE_HOST_PASSTHROUGH, KVMConstant.CPU_MODE_HOST_MODEL, "Kunpeng-920", "FT-2000+", "Tengyun-S2500"));
    }

    public String getArchitecture() {
        return CpuArchitecture.aarch64.name();
    }
}
