package org.zstack.storage.volume;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.volume.VolumeVO;
import org.zstack.resourceconfig.BindResourceConfig;

/**
 */
@GlobalConfigDefinition
public class VolumeGlobalConfig {
    public static final String CATEGORY = "volume";

    @GlobalConfigValidation
    public static GlobalConfig UPDATE_DISK_OFFERING_TO_NULL_WHEN_DELETING = new GlobalConfig(CATEGORY, "diskOffering.setNullWhenDeleting");

    @GlobalConfigValidation(validValues = {"Direct","Delay", "Never"})
    public static GlobalConfig VOLUME_DELETION_POLICY = new GlobalConfig(CATEGORY, "deletionPolicy");

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig VOLUME_EXPUNGE_PERIOD = new GlobalConfig(CATEGORY, "expungePeriod");

    @GlobalConfigValidation(min = 1)
    public static GlobalConfig VOLUME_EXPUNGE_INTERVAL = new GlobalConfig(CATEGORY, "expungeInterval");

    @GlobalConfigValidation(min = 600)
    public static GlobalConfig REFRESH_VOLUME_SIZE_INTERVAL = new GlobalConfig(CATEGORY, "refreshVolumeSizeInterval");

    @GlobalConfigValidation(min = 1, max = 100)
    public static GlobalConfig HOST_COUNT_PER_BATCH_REFRESH_VOLUME_SIZE = new GlobalConfig(CATEGORY, "refreshVolumeSize.hostCountPerBatch");

    @GlobalConfigValidation(validValues = {"AllActive", "Monitored", "None"})
    public static GlobalConfig AUTO_REFRESH_VOLUME_SCOPE = new GlobalConfig(CATEGORY, "refreshVolumeSize.scope");

    @GlobalConfigValidation(validValues = {"0", "512", "4096"})
    @GlobalConfigDef(defaultValue = "0", type = Integer.class, description = "physical block size of the underlying storage")
    @BindResourceConfig({VolumeVO.class, PrimaryStorageVO.class})
    public static GlobalConfig VOLUME_PHYSICAL_BLOCK_SIZE = new GlobalConfig(CATEGORY, "physical.block.size");

    public static GlobalConfig AUTO_SNAPSHOT_BEFORE_CHANGE_OPERATION = new GlobalConfig(CATEGORY, "auto.snapshot.before.change");
}
