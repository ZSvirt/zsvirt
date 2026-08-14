package org.zstack.storage.primary;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.resourceconfig.BindResourceConfig;

/**
 */
@GlobalConfigDefinition
public class PrimaryStorageGlobalConfig {
    public static final String CATEGORY = "primaryStorage";

    @GlobalConfigValidation(min = 1)
    public static GlobalConfig IMAGE_CACHE_GARBAGE_COLLECTOR_INTERVAL = new GlobalConfig(CATEGORY, "imageCache.garbageCollector.interval");
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig PRIMARY_STORAGE_DELETEBITS_GARBAGE_COLLECTOR_INTERVAL =
            new GlobalConfig(CATEGORY, "primarystorage.delete.bits.garbageCollector.interval");
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig PRIMARY_STORAGE_DELETEBITS_TIMES =
            new GlobalConfig(CATEGORY, "primarystorage.delete.bits.times");
    @GlobalConfigValidation
    public static GlobalConfig PRIMARY_STORAGE_DELETEBITS_ON = new GlobalConfig(CATEGORY, "primarystorage.delete.bits.garbage.on");
    @GlobalConfigValidation
    public static GlobalConfig RESERVED_CAPACITY = new GlobalConfig(CATEGORY, "reservedCapacity");
    @GlobalConfigValidation(min = 1)
    public static GlobalConfig PING_INTERVAL = new GlobalConfig(CATEGORY, "ping.interval");
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig PING_PARALLELISM_DEGREE = new GlobalConfig(CATEGORY, "ping.parallelismDegree");
    @GlobalConfigValidation(min = 0)
    @BindResourceConfig({PrimaryStorageVO.class})
    public static GlobalConfig PRIMARY_STORAGE_AUTO_DELETE_TRASH = new GlobalConfig(CATEGORY, "primarystorage.auto.delete.trash.interval");

    @GlobalConfigValidation(min = 0)
    @BindResourceConfig({PrimaryStorageVO.class})
    public static GlobalConfig TRASH_EXPIRATION_TIME = new GlobalConfig(CATEGORY, "trash.expiration.time");

    @GlobalConfigValidation(inNumberRange = {1, 255})
    @GlobalConfigDef(defaultValue = "10", type = Integer.class,  description = "allocator concurrency level, if enabled.")
    public static GlobalConfig ALLOCATE_PRIMARYSTORAGE_CONCURRENCY = new GlobalConfig(CATEGORY, "allocate.primaryStore.Concurrency");

    @GlobalConfigValidation()
    @BindResourceConfig(value = {PrimaryStorageVO.class})
    @GlobalConfigDef(defaultValue = "0.9", type = Double.class, description = "The threshold for predicting primary storage's used physical capacity. " +
            "If the predicted value exceeds this threshold, it indicates that the primary storage is expected to be full in the future.")
    public static GlobalConfig PRIMARY_STORAGE_USED_PHYSICAL_CAPACITY_FORECAST_THRESHOLD =
            new GlobalConfig(CATEGORY, "primaryStorage.used.physicalCapacity.forecast.threshold");

    @GlobalConfigValidation(validValues = {"true", "false"})
    @BindResourceConfig({PrimaryStorageVO.class})
    @GlobalConfigDef(defaultValue = "true", type = Boolean.class, description = "Whether undo temp snapshot after template uploaded")
    public static GlobalConfig UNDO_TEMP_SNAPSHOT = new GlobalConfig(CATEGORY, "undo.tempSnapshot");
    @GlobalConfigValidation(min = 1)
    @GlobalConfigDef(defaultValue = "1", type = Long.class)
    public static GlobalConfig COLLECT_AND_FORECAST_INTERVAL = new GlobalConfig(CATEGORY, "collect.forecast.interval");

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig PRIMARY_STORAGE_HOST_STATUS_REFRESH_INTERVAL = new GlobalConfig(CATEGORY, "primarystorage.host.status.refresh.interval");
}
