package org.zstack.header.network.l2;

public interface L2NetworkDeleteExtensionPoint {
    void preDeleteL2Network(L2NetworkInventory inventory) throws L2NetworkException;

    void beforeDeleteL2Network(L2NetworkInventory inventory);

    void afterDeleteL2Network(L2NetworkInventory inventory);
}
