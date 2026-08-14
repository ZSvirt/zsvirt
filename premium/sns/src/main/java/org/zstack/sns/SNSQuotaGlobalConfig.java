package org.zstack.sns;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.identity.QuotaGlobalConfig;

/**
 * @Author: fubang
 * @Date: 2018/6/20
 */
@GlobalConfigDefinition
public class SNSQuotaGlobalConfig extends QuotaGlobalConfig {
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig SNS_ENDPOINT_NUM = new GlobalConfig(CATEGORY, SNSQuotaConstant.SNS_ENDPOINT_NUM);
}
