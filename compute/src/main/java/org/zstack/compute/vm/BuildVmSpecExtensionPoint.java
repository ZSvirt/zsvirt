package org.zstack.compute.vm;

import org.zstack.header.vm.VmInstanceSpec;

public interface BuildVmSpecExtensionPoint {
    void afterBuildVmSpec(VmInstanceSpec spec);
}
