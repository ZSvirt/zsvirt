package org.zstack.storage.backup.imagestore;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.mevoco.PremiumGlobalConfig;
import org.zstack.resourceconfig.BindResourceConfig;

/**
 * Created by MaJin on 2019/4/18.
 */
@GlobalConfigDefinition
public class ImageStoreGlobalConfig {
    public static final String CATEGORY = "imagestore";

    @GlobalConfigValidation(min = -1)
    public static PremiumGlobalConfig RECLAIM_INTERVAL = new PremiumGlobalConfig(CATEGORY, "reclaim.interval");

    @GlobalConfigValidation(min = -1)
    @BindResourceConfig(value = {BackupStorageVO.class})
    public static PremiumGlobalConfig MAX_CAPACITY = new PremiumGlobalConfig(CATEGORY, "max.capacity");

    @GlobalConfigDef(defaultValue = "false", type = Boolean.class, description = "Whether clean ImageStore on expunge")
    public static PremiumGlobalConfig CLEAN_IMAGESTORE_ON_EXPUNGE = new PremiumGlobalConfig(CATEGORY, "cleanOnExpunge");

    @GlobalConfigValidation
    public static PremiumGlobalConfig ZSTORE_ALLOW_PORTS = new PremiumGlobalConfig(CATEGORY, "zstore.allow.ports");

    @GlobalConfigValidation(min = 1, max = 32)
    public static GlobalConfig IMAGE_STORE_SYNC_LEVEL = new GlobalConfig(CATEGORY, "imageStore.syncLevel");

    @GlobalConfigValidation(min = 1, max = 16)

    @BindResourceConfig(value = {BackupStorageVO.class})
    public static GlobalConfig BLOB_UPLOAD_CONCURRENCY = new GlobalConfig(CATEGORY, "blob.upload.concurrency");

    @GlobalConfigValidation(min = 1, max = 16)
    @BindResourceConfig(value = {BackupStorageVO.class})
    public static GlobalConfig BLOB_DOWNLOAD_CONCURRENCY = new GlobalConfig(CATEGORY, "blob.download.concurrency");
}
