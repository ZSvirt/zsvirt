package org.zstack.core.progress;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * Created by xing5 on 2017/5/4.
 */
@GlobalConfigDefinition
public class ProgressGlobalConfig {
    public static final String CATEGORY = "progress";

    @Deprecated
    @GlobalConfigValidation
    public static GlobalConfig PROGRESS_ON = new GlobalConfig(CATEGORY, "progress.on");
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig PROGRESS_TTL_SECONDS = new GlobalConfig(CATEGORY, "progress.ttl");
    @GlobalConfigValidation(min = 1)
    public static GlobalConfig CLEANUP_THREAD_INTERVAL = new GlobalConfig(CATEGORY, "progress.cleanupThreadInterval");
}
