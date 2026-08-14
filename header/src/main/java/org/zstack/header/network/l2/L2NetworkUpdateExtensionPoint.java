package org.zstack.header.network.l2;

/**
 * Created by boce.wang on 03/25/2024.
 */
public interface L2NetworkUpdateExtensionPoint {

    void beforeUpdateL2NetworkVirtualNetworkId(L2NetworkInventory l2Inv);

    void afterUpdateL2NetworkVirtualNetworkId(L2NetworkInventory l2Inv);
}
