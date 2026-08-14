package org.zstack.core.cascade;

import org.zstack.header.core.Completion;

/**
 */
public abstract class AbstractCascadeExtension implements CascadeExtensionPoint {
    @Override
    public void syncCascade(CascadeAction action) throws CascadeException {
    }

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        completion.success();
    }
}
