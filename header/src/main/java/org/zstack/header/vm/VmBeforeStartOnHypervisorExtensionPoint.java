package org.zstack.header.vm;

/**
 */
public interface VmBeforeStartOnHypervisorExtensionPoint {
    void beforeStartVmOnHypervisor(VmInstanceSpec spec);
}
