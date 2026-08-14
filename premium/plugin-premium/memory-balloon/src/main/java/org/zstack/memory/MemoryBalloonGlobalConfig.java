package org.zstack.memory;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

@GlobalConfigDefinition
public class MemoryBalloonGlobalConfig {
    public static final String CATEGORY = "vmMemory";

    @GlobalConfigValidation(inNumberRange = {60, 600})
    @GlobalConfigDef(defaultValue = "300", type = Integer.class,  description = "memory auto balloon scan interval")
    public static GlobalConfig MEMORY_AUTO_BALLOON_SCAN_INTERVAL = new GlobalConfig(CATEGORY, "memory.auto.balloon.scan.interval");

    @GlobalConfigValidation(validValues = {
            "IdleFirst",
            "UrgentFirst",
            "All"
    })
    @GlobalConfigDef(defaultValue = "All", description = "memory auto balloon strategy")
    public static GlobalConfig MEMORY_AUTO_BALLOON_STRATEGY = new GlobalConfig(CATEGORY, "memory.auto.balloon.strategy");

    @GlobalConfigValidation(inNumberRange = {0, 100})
    @GlobalConfigDef(defaultValue = "5", description = "memory auto balloon threshold to avoid frequent balloon")
    public static GlobalConfig MEMORY_AUTO_BALLOON_THRESHOLD_PERCENT = new GlobalConfig(CATEGORY, "memory.auto.balloon.threshold.percent");
}
