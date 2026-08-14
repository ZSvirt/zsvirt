package org.zstack.billing.userconfig;

import org.springframework.util.StringUtils;
import org.zstack.billing.BillingSystemTags;
import org.zstack.billing.userconfig.diskoffering.BillingDiskOfferingUserConfig;
import org.zstack.billing.userconfig.instanceoffering.BillingInstanceOfferingUserConfig;
import org.zstack.billing.userconfig.price.ResourcePriceUserConfig;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.gson.JSONObjectUtil;

/**
 * Created by lining on 2019/4/17.
 */
public class BillingUserConfigUtils {
    public final static ResourcePriceUserConfig getResourcePriceConfig(String priceUuid) {
        String configStr = BillingSystemTags.PRICE_USER_CONFIG
                .getTokenByResourceUuid(priceUuid, BillingSystemTags.PRICE_USER_CONFIG_TOKEN);
        DebugUtils.Assert(!StringUtils.isEmpty(configStr), "price userConfig is null");

        ResourcePriceUserConfig config = JSONObjectUtil.toObject(configStr, ResourcePriceUserConfig.class);
        return config;
    }
}
