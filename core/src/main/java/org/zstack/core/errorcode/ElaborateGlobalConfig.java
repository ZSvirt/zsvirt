package org.zstack.core.errorcode;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * Created by mingjian.deng on 2018/12/19.
 */
@GlobalConfigDefinition
public class ElaborateGlobalConfig {
    public static final String CATEGORY = "elaborate";

    @GlobalConfigValidation
    public static GlobalConfig ELIMILATE_TIME = new GlobalConfig(CATEGORY, "elimilate.time");
}
