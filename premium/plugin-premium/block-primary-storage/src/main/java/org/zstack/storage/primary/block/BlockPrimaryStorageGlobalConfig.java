package org.zstack.storage.primary.block;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.mevoco.PremiumGlobalConfig;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2023/3/22 21:03
 */
@GlobalConfigDefinition
public class BlockPrimaryStorageGlobalConfig {
    public static final String CATEGORY = "blockPrimaryStorage";

    @GlobalConfigValidation(min = 10240)
    @GlobalConfigDef(defaultValue = "1048576", type = Integer.class, description = "heartbeat lun size of block primary storage")
    public static GlobalConfig BLOCK_PRIMARY_STORAGE_HEARTBEAT_LUN_SIZE = new PremiumGlobalConfig(CATEGORY, "heartbeat.lun.size");

    @GlobalConfigValidation(min = 10485760)
    @GlobalConfigDef(defaultValue = "10485760", type = Integer.class, description = "extra lun size for downloading image cache")
    public static GlobalConfig BLOCK_PRIMARY_STORAGE_EXTRA_LUN_SIZE_FOR_IMAGE_CACHE = new PremiumGlobalConfig(CATEGORY, "extra.lun.size");

    @GlobalConfigDef(defaultValue = "/tmp/.imagecache/tmp/", type = String.class, description = "tmp image cache folder for downloading")
    public static GlobalConfig BLOCK_PRIMARY_STORAGE_TMP_FOLDER_FOR_IMAGE_CACHE = new PremiumGlobalConfig(CATEGORY, "tmp.image.cache.folder");

    @GlobalConfigValidation
    @GlobalConfigDef(defaultValue = "3600", type = Long.class, description = "the interval to garbage collect stale volumes, in seconds")
    public static GlobalConfig GC_INTERVAL = new GlobalConfig(CATEGORY, "deletion.gcInterval");
}
