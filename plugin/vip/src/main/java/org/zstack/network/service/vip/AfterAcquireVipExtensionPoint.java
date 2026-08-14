package org.zstack.network.service.vip;

public interface AfterAcquireVipExtensionPoint {
    void afterAcquireVip(VipInventory vipInventory);
}
