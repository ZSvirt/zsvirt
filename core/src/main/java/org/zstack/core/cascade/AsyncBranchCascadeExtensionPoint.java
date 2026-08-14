package org.zstack.core.cascade;

import org.zstack.header.core.Completion;

public interface AsyncBranchCascadeExtensionPoint {
    void asyncCascade(CascadeAction action, Completion completion);

    String getCascadeResourceName();

    boolean skipOriginCascadeExtension(CascadeAction action);
}
