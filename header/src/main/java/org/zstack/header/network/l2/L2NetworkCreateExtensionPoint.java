package org.zstack.header.network.l2;

import org.zstack.header.network.NetworkException;

public interface L2NetworkCreateExtensionPoint {
    void beforeCreateL2Network(APICreateL2NetworkMsg msg) throws NetworkException;

    void afterCreateL2Network(L2NetworkInventory l2Network);
}
