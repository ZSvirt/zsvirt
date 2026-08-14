package org.zstack.network.service.vip;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.identity.QuotaGlobalConfig;

/**
 * Created by kayo on 2018/4/21.
 */
@GlobalConfigDefinition
public class VipQuotaGlobalConfig extends QuotaGlobalConfig {
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig VIP_NUM = new GlobalConfig(CATEGORY, VipQuotaConstant.VIP_NUM);
}
