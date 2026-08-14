package org.zstack.network.l2;

import org.zstack.header.host.HypervisorType;
import org.zstack.header.network.l2.*;

public interface L2NetworkManager {
    L2NetworkFactory getL2NetworkFactory(L2NetworkType type);

    L2NetworkRealizationExtensionPoint getRealizationExtension(L2NetworkType l2Type, HypervisorType hvType);

    L2NetworkRealizationExtensionPoint getRealizationExtension(L2NetworkType l2Type, L2ProviderType providerType);

    L2NetworkRealizationExtensionPoint getRealizationExtension(L2NetworkType l2Type, HypervisorType hvType, String providerType);

    L2NetworkAttachClusterExtensionPoint getAttachClusterExtension(L2NetworkType l2Type, HypervisorType hvType);
}
