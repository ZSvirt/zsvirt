package org.zstack.header.network.l2;

import org.zstack.header.core.Completion;
import org.zstack.header.host.HypervisorType;

/**
 * Created by GuoYi on 5/3/20.
 */
public interface L2NetworkAttachClusterExtensionPoint {
    L2NetworkType getSupportedL2NetworkType();
    HypervisorType getSupportedHypervisorType();

    void beforeAttach(L2NetworkInventory l2Network, String hostUuid, Completion completion);
    void afterAttach(L2NetworkInventory l2Network, String hostUuid, Completion completion);
}
