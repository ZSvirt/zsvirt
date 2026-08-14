package org.zstack.scheduler;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * Created by MaJin on 2019/3/12.
 */
@GlobalConfigDefinition
public class SchedulerGlobalConfig {
    public static final String CATEGORY = "scheduler";

    @GlobalConfigValidation(min = 1)
    public static GlobalConfig ATTACHED_TRIGGERS_LIMIT = new GlobalConfig(CATEGORY, "job.attached.triggers.limit");


    @GlobalConfigValidation(min = 1)
    public static GlobalConfig CONTAIN_RESOURCES_LIMIT = new GlobalConfig(CATEGORY, "job.resources.limit");

    @GlobalConfigValidation(validValues = {"true", "false"})
    public static GlobalConfig LAST_JOB_UNCOMPLETED_SKIP_NEXT_JOB =
            new GlobalConfig(CATEGORY, "last.job.uncompleted.skip.next.job");
}
