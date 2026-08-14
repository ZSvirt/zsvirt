package org.zstack.header.host;

import org.zstack.header.core.Completion;

/**
 */
public interface HostAddExtensionPoint {
    void beforeAddHost(HostInventory host, Completion completion);

    void afterAddHost(HostInventory host, Completion completion);
}
