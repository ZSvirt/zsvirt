package org.zstack.header.vm;

import org.zstack.header.core.NoErrorCompletion;

public interface ReleaseNetworkServiceOnDeletingNicExtensionPoint {
    void releaseNetworkServiceOnDeletingNic(VmNicInventory nic, NoErrorCompletion completion);
}
