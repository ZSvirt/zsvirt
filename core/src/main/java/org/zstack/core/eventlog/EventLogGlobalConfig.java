package org.zstack.core.eventlog;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

@GlobalConfigDefinition
public class EventLogGlobalConfig {
    public static final String CATEGORY = "eventlog";

    @GlobalConfigValidation(inNumberRange = {0, 180})
    @GlobalConfigDef(defaultValue = "90", type = Integer.class, description = "How long a log entry can stay in database")
    public static GlobalConfig EXPIRE_TIME_IN_DAY = new GlobalConfig(CATEGORY, "expireTimeInDay");
}
