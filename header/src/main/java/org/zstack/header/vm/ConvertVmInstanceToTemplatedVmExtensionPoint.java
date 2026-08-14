package org.zstack.header.vm;

import org.zstack.header.core.Completion;

public interface ConvertVmInstanceToTemplatedVmExtensionPoint {
    /**
     * Call before converting a VM instance into a templated VM instance.
     */
    void beforeConvertVmInstanceToTemplatedVm(String vmUuid, Completion completion);
}
