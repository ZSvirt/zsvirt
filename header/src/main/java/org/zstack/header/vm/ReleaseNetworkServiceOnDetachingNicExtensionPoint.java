package org.zstack.header.vm;

import org.zstack.header.core.NoErrorCompletion;

/**
 * Created by frank on 7/18/2015.
 */
public interface ReleaseNetworkServiceOnDetachingNicExtensionPoint {
    void releaseResourceOnDetachingNic(VmInstanceSpec spec, VmNicInventory nic, NoErrorCompletion completion);
}
