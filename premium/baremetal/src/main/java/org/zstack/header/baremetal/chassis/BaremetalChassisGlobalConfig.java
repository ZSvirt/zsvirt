package org.zstack.header.baremetal.chassis;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * Created by GuoYi on 2018-10-15.
 */
@GlobalConfigDefinition
public class BaremetalChassisGlobalConfig {
    public static final String CATEGORY = "baremetalChassis";

    @GlobalConfigValidation(min = 1)
    public static GlobalConfig BATCH_CREATE_CHASSIS_MAX_NUMBER = new GlobalConfig(CATEGORY, "batch.maxnumber");

    @GlobalConfigValidation(min = 1)
    @GlobalConfigDef(defaultValue = "10", type = Integer.class,  description = "timeout in minute when get chassis hardware info")
    public static GlobalConfig GET_CHASSIS_HW_INFO_TIMEOUT = new GlobalConfig(CATEGORY, "get.chassis.hw.info.timeout");
}
