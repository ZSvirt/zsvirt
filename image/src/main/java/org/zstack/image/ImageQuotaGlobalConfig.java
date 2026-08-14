package org.zstack.image;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.identity.QuotaGlobalConfig;

/**
 * Created by kayo on 2018/4/21.
 */
@GlobalConfigDefinition
public class ImageQuotaGlobalConfig extends QuotaGlobalConfig {

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig IMAGE_NUM = new GlobalConfig(CATEGORY, ImageQuotaConstant.IMAGE_NUM);
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig IMAGE_SIZE = new GlobalConfig(CATEGORY, ImageQuotaConstant.IMAGE_SIZE);
}
