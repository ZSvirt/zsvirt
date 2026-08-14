package org.zstack.header.baremetal.pxeserver;

/**
 * Created by GuoYi on 2018-10-30.
 */
public interface BaremetalPxeServerDetachExtensionPoint {
    void preDetachBaremetalPxeServer(BaremetalPxeServerInventory inventory, String clusterUuid);
    void beforeDetachBaremetalPxeServer(BaremetalPxeServerInventory inventory, String clusterUuid);
    void failToDetachBaremetalPxeServer(BaremetalPxeServerInventory inventory, String clusterUuid);
    void afterDetachBaremetalPxeServer(BaremetalPxeServerInventory inventory, String clusterUuid);
}
