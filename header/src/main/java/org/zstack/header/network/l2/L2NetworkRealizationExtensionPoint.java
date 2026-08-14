package org.zstack.header.network.l2;

import org.zstack.header.core.Completion;
import org.zstack.header.host.HypervisorType;

public interface L2NetworkRealizationExtensionPoint {
    void realize(L2NetworkInventory l2Network, String hostUuid, Completion completion);

    default void realize(L2NetworkInventory l2Network, String hostUuid, boolean noStatusCheck, Completion completion) {
        completion.success();
    }

    void check(L2NetworkInventory l2Network, String hostUuid, Completion completion);

    default void update(L2NetworkInventory oldL2Network, L2NetworkInventory newL2Network, String hostUuid, Completion completion) {
        completion.success();
    }

    L2NetworkType getSupportedL2NetworkType();

    HypervisorType getSupportedHypervisorType();

    default L2ProviderType getL2ProviderType() {
        return null;
    }

    void delete(L2NetworkInventory l2Network, String hostUuid, Completion completion);
}
