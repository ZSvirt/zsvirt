package org.zstack.storage.snapshot;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 */
@GlobalConfigDefinition
public class VolumeSnapshotGlobalConfig {
    public static final String CATEGORY = "volumeSnapshot";

    @GlobalConfigValidation(min = 0, max = 120)
    public static GlobalConfig MAX_INCREMENTAL_SNAPSHOT_NUM = new GlobalConfig(CATEGORY, "incrementalSnapshot.maxNum");
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig SNAPSHOT_DELETE_PARALLELISM_DEGREE = new GlobalConfig(CATEGORY, "delete.parallelismDegree");
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig SNAPSHOT_BACKUP_PARALLELISM_DEGREE = new GlobalConfig(CATEGORY, "backup.parallelismDegree");
    @GlobalConfigValidation
    public static GlobalConfig SNAPSHOT_BEFORE_REVERTVOLUME = new GlobalConfig(CATEGORY, "snapshot.before.revertvolume");

    @GlobalConfigValidation(validValues = {"true", "false"})
    @GlobalConfigDef(defaultValue = "true", type = Boolean.class, description = "effective count with detached volumes")
    public static GlobalConfig EFFECTIVE_COUNT_WITH_DETACHED_VOLUMES = new GlobalConfig(CATEGORY, "effectiveCount.withDetachedVolumes");
}
