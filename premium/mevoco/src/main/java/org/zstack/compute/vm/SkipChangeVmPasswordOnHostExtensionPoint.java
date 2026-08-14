package org.zstack.compute.vm;

import org.zstack.header.vm.VmInstanceSpec;

public interface SkipChangeVmPasswordOnHostExtensionPoint {
    boolean skipChangeVmPasswordOnHost(VmInstanceSpec spec);
}
