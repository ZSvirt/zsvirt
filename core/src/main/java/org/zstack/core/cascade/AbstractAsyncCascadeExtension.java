package org.zstack.core.cascade;

/**
 */
public abstract class AbstractAsyncCascadeExtension implements CascadeExtensionPoint {
    @Override
    public void syncCascade(CascadeAction action) throws CascadeException {
    }
}
