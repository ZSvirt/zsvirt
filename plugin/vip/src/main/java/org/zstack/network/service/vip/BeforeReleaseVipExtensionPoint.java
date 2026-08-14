package org.zstack.network.service.vip;

public interface BeforeReleaseVipExtensionPoint {
    void beforeReleaseVip(VipInventory vipInventory);
}
