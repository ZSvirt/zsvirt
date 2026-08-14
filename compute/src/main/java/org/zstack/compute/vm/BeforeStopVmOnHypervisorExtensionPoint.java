package org.zstack.compute.vm;

import org.zstack.header.vm.StopVmOnHypervisorMsg;
import org.zstack.header.vm.VmInstanceSpec;

public interface BeforeStopVmOnHypervisorExtensionPoint {
    void beforeStopVmOnHypervisor(VmInstanceSpec spec, StopVmOnHypervisorMsg msg);
}
