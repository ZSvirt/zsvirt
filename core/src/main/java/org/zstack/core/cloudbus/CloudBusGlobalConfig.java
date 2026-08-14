package org.zstack.core.cloudbus;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 */
@GlobalConfigDefinition
public class CloudBusGlobalConfig {
    public static final String CATEGORY = "cloudBus";

    @GlobalConfigValidation
    public static GlobalConfig STATISTICS_ON = new GlobalConfig(CATEGORY, "statistics.on");
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig MAX_MANAGEMENTNODE_NOTFOUND_ERROR_HANDLER_NUM = new GlobalConfig(CATEGORY, "managementNodeNotFoundHandler.maxNum");
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig MAX_MANAGEMENTNODE_NOTFOUND_ERROR_HANDLER_TIMEOUT = new GlobalConfig(CATEGORY, "managementNodeNotFoundHandler.timeoutInSecs");

    /**
     * validValues: {-1, 0, 1}
     *  -1: configuration does not take effect
     *  0: off read API log
     *  1: open read API log
     */
    @GlobalConfigValidation(inNumberRange = {-1, 1})
    @GlobalConfigDef(type = Long.class, defaultValue = "-1", description = "open read API log")
    public static GlobalConfig OPEN_READ_API_LOG = new GlobalConfig(CATEGORY, "openReadAPILog");
}
