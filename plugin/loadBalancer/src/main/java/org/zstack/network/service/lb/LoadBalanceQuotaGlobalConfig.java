package org.zstack.network.service.lb;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.identity.QuotaGlobalConfig;

/**
 * Created by kayo on 2018/4/21.
 */
@GlobalConfigDefinition
public class LoadBalanceQuotaGlobalConfig extends QuotaGlobalConfig {

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig LOAD_BALANCER_NUM = new GlobalConfig(CATEGORY, LoadBalanceQuotaConstant.LOAD_BALANCER_NUM);

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig LOAD_BALANCER_LISTENER_NUM = new GlobalConfig(CATEGORY, LoadBalanceQuotaConstant.LOAD_BALANCER_LISTENER_NUM);
}
