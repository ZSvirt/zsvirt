package org.zstack.billing.spendingcalculator.volume.root;

import org.zstack.billing.BillingSystemTags;
import org.zstack.billing.ResourceCreateUsageExtensionPoint;
import org.zstack.billing.Usage;

/**
 * Created by lining on 2019/4/17.
 */
public class RootVolumeCreateUsageExtensionPoint implements ResourceCreateUsageExtensionPoint {
    @Override
    public Usage makeUsage(Usage usage) {
        if (!(usage instanceof RootVolumeUsageVO)) {
            return null;
        }

        RootVolumeUsageVO usageVO = (RootVolumeUsageVO) usage;
        if (!BillingSystemTags.VOLUME_PRICE_USER_CONFIG.hasTag(usageVO.getVolumeUuid())) {
            return usage;
        }

        RootVolumeUsageExtensionVO newUsageVO = new RootVolumeUsageExtensionVO(usageVO);
        String priceUserConfig = BillingSystemTags.VOLUME_PRICE_USER_CONFIG.getTokenByResourceUuid(usageVO.getVolumeUuid(), BillingSystemTags.VOLUME_PRICE_USER_CONFIG_TOKEN);
        newUsageVO.setResourcePriceUserConfig(priceUserConfig);

        return newUsageVO;
    }
}
