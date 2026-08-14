package org.zstack.network.service.flat;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * Created by kayo on 2018/5/4.
 */
@GlobalConfigDefinition
public class FlatNetwordProviderGlobalConfig {
    public static final String CATRGORY = "flatNetworkProvider";

    @GlobalConfigValidation
    public static GlobalConfig ALLOW_DEFAULT_DNS = new GlobalConfig(CATRGORY, "allow.default.dns");
}
