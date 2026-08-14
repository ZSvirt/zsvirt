package org.zstack.tag2;


import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.identity.QuotaGlobalConfig;

@GlobalConfigDefinition
public class Tag2QuotaGlobalConfig extends QuotaGlobalConfig {

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig TAG_PATTERN_TOTAL_NUM = new GlobalConfig(CATEGORY, Tag2QuotaConstant.TAG_PATTERN_TOTAL_NUM);
}
