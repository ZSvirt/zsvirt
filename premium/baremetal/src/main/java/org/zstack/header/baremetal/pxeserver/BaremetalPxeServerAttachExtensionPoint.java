package org.zstack.header.baremetal.pxeserver;

/**
 * Created by GuoYi on 2019-01-23.
 */
public interface BaremetalPxeServerAttachExtensionPoint {
    void preAttachBaremetalPxeServer(BaremetalPxeServerInventory inventory, String clusterUuid);
    void beforeAttachBaremetalPxeServer(BaremetalPxeServerInventory inventory, String clusterUuid);
    void failToAttachBaremetalPxeServer(BaremetalPxeServerInventory inventory, String clusterUuid);
    void afterAttachBaremetalPxeServer(BaremetalPxeServerInventory inventory, String clusterUuid);
}
