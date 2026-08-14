package org.zstack.scheduler.snapshot;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

@GlobalPropertyDefinition
public class SchedulerSnapshotGlobalProperty {
    @GlobalProperty(name = "AddTagToSchedulerSnapshot", defaultValue = "false")
    public static boolean ADD_TAG_TO_SCHEDULER_SNAPSHOT;
}
