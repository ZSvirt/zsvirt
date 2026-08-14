package org.zstack.header.vm;

import org.zstack.header.core.Completion;

public interface VmReleaseResourceExtensionPoint {
    void releaseVmResource(VmInstanceSpec spec, Completion completion);
}
