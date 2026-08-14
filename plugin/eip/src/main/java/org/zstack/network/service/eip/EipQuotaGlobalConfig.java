package org.zstack.network.service.eip;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.identity.QuotaGlobalConfig;

/**
 * Created by kayo on 2018/4/21.
 */
@GlobalConfigDefinition
public class EipQuotaGlobalConfig extends QuotaGlobalConfig {

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig EIP_NUM = new GlobalConfig(CATEGORY, EipQuotaConstant.EIP_NUM);
}
