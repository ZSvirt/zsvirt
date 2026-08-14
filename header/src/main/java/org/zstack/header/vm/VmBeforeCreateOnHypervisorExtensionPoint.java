package org.zstack.header.vm;

/**
 */
public interface VmBeforeCreateOnHypervisorExtensionPoint {
    void beforeCreateVmOnHypervisor(VmInstanceSpec spec);
}
