package org.zstack.header.scheduler;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Created by MaJin on 2020/4/3.
 */

@GlobalPropertyDefinition
public class SchedulerGlobalProperty {
    @GlobalProperty(name="upgradeSchedulerJobHistory", defaultValue = "false")
    public static boolean UPGRADE_SCHEDULER_JOB_HISTORY;
}

