package org.zstack.header.network.l3;

public interface L3NetworkDeleteExtensionPoint {
    String preDeleteL3Network(L3NetworkInventory inventory) throws L3NetworkException;

    void beforeDeleteL3Network(L3NetworkInventory inventory);

    void afterDeleteL3Network(L3NetworkInventory inventory);
}
