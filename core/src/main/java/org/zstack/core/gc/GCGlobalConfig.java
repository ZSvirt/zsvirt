package org.zstack.core.gc;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * Created by xing5 on 2017/3/1.
 */
@GlobalConfigDefinition
public class GCGlobalConfig {
    public static final String CATEGORY = "gc";

    @GlobalConfigValidation(min = 1)
    public static GlobalConfig SCAN_ORPHAN_JOB_INTERVAL = new GlobalConfig(CATEGORY, "orphanJobScanInterval");

    @GlobalConfigValidation(min = 1)
    public static GlobalConfig CLEAN_UP_COMPLETED_JOB_INTERVAL = new GlobalConfig(CATEGORY, "cleanUpCompletedJobInterval");

    @GlobalConfigValidation
    public static GlobalConfig RETENTION_TIME = new GlobalConfig(CATEGORY, "retentionTime");
}
