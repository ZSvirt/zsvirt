package org.zstack.network.service.userdata;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * Created by lining on 2019/3/15.
 */
@GlobalConfigDefinition
public class UserdataGlobalConfig {
    public static final String CATEGORY = "userdata";

    @GlobalConfigValidation
    public static GlobalConfig OPEN_USERDATA_SERVICE_BY_DEFAULT = new GlobalConfig(CATEGORY, "userdata.openServiceByDefault");
}
