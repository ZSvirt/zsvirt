package org.zstack.header.host;

import org.zstack.header.core.Completion;

/**
 */
public abstract class AbstractHostAddExtensionPoint implements HostAddExtensionPoint {
    @Override
    public void beforeAddHost(HostInventory host, Completion completion) {
        completion.success();
    }

    @Override
    public void afterAddHost(HostInventory host, Completion completion) {
        completion.success();
    }
}
