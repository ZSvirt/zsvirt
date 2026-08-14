package org.zstack.core.cascade;

import org.zstack.header.core.Completion;

/**
 */
public abstract class AbstractSyncCascadeExtension implements CascadeExtensionPoint {
    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        completion.success();
    }
}
