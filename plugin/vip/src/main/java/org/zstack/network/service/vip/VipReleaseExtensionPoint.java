package org.zstack.network.service.vip;

import org.zstack.header.core.Completion;

public interface VipReleaseExtensionPoint {
    String getVipUse();
    
    void releaseServicesOnVip(VipInventory vip, Completion completion);
}
