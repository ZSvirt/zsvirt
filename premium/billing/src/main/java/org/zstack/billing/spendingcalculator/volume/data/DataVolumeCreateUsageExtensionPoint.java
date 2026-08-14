package org.zstack.billing.spendingcalculator.volume.data;

import org.zstack.billing.BillingSystemTags;
import org.zstack.billing.ResourceCreateUsageExtensionPoint;
import org.zstack.billing.Usage;

/**
 * Created by lining on 2019/4/17.
 */
public class DataVolumeCreateUsageExtensionPoint implements ResourceCreateUsageExtensionPoint {
    @Override
    public Usage makeUsage(Usage usage) {
        if (!(usage instanceof DataVolumeUsageVO)) {
            return null;
        }

        DataVolumeUsageVO usageVO = (DataVolumeUsageVO) usage;
        if (!BillingSystemTags.VOLUME_PRICE_USER_CONFIG.hasTag(usageVO.getVolumeUuid())) {
            return usage;
        }

        DataVolumeUsageExtensionVO newUsageVO = new DataVolumeUsageExtensionVO(usageVO);
        String priceUserConfig =  BillingSystemTags.VOLUME_PRICE_USER_CONFIG.getTokenByResourceUuid(usageVO.getVolumeUuid(), BillingSystemTags.VOLUME_PRICE_USER_CONFIG_TOKEN);
        newUsageVO.setResourcePriceUserConfig(priceUserConfig);

        return newUsageVO;
    }
}
