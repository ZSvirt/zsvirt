package org.zstack.storage.primary;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Created by xing5 on 2016/5/10.
 */
@GlobalPropertyDefinition
public class PrimaryStorageGlobalProperty {
    @GlobalProperty(name="PrimaryStorage.capacityTrackerOn", defaultValue = "false")
    public static boolean CAPACITY_TRACKER_ON;

    @Deprecated
    @GlobalProperty(name="PrimaryStorage.incremental.cache.directUseVolumeSnapshot", defaultValue = "true")
    public static boolean USE_SNAPSHOT_AS_INCREMENTAL_CACHE;
}
