package org.zstack.header.vm;

import org.zstack.header.core.Completion;

public interface PostVmInstantiateResourceExtensionPoint {
    void postBeforeInstantiateVmResource(VmInstanceSpec spec);

    void postInstantiateVmResource(VmInstanceSpec spec, Completion completion);

    void postReleaseVmResource(VmInstanceSpec spec, Completion completion);
}
