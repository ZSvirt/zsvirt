package org.zstack.header.host;

import org.zstack.header.core.workflow.Flow;

public interface HostAfterUpdatedExtensionPoint {
    Flow afterHostUpdated(HostInventory oldHost, HostInventory newHost);
}
