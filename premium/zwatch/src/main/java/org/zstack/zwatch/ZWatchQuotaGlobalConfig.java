package org.zstack.zwatch;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.identity.QuotaGlobalConfig;

/**
 * @Author: fubang
 * @Date: 2018/6/20
 */
@GlobalConfigDefinition
public class ZWatchQuotaGlobalConfig extends QuotaGlobalConfig{
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig ZWATCH_ALARM_NUM = new GlobalConfig(CATEGORY, ZWatchQuotaConstant.ZWATCH_ALARM_NUM);

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig ZWATCH_EVENT_NUM = new GlobalConfig(CATEGORY, ZWatchQuotaConstant.ZWATCH_EVENT_NUM);
}
