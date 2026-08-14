package org.zstack.header.vm;

import org.zstack.header.core.Completion;

public interface PreVmInstantiateResourceExtensionPoint {
    void preBeforeInstantiateVmResource(VmInstanceSpec spec) throws VmInstantiateResourceException;

    void preInstantiateVmResource(VmInstanceSpec spec, Completion completion);

    void preReleaseVmResource(VmInstanceSpec spec, Completion completion);
}
