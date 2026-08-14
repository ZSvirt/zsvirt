package org.zstack.header.network.l3;

public interface L3NetworkCreateExtensionPoint {
    void afterCreateL3Network(L3NetworkInventory inventory);
}
