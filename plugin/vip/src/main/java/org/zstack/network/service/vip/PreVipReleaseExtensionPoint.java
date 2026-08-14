package org.zstack.network.service.vip;

import org.zstack.header.core.Completion;

/**
 * Created by liangbo.zhou on 17-6-22.
 */
public interface PreVipReleaseExtensionPoint {
    void preReleaseServicesOnVip(VipInventory vip, Completion completion);
}
