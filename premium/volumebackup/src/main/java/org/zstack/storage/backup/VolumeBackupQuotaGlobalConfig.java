package org.zstack.storage.backup;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.identity.QuotaGlobalConfig;

/**
 * Created by Qi Le on 2020/7/3
 */
public class VolumeBackupQuotaGlobalConfig extends QuotaGlobalConfig {

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig VOLUME_BACKUP_NUM = new GlobalConfig(CATEGORY, VolumeBackupQuotaConstant.VOLUME_BACKUP_NUM);

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig VOLUME_BACKUP_SIZE = new GlobalConfig(CATEGORY, VolumeBackupQuotaConstant.VOLUME_BACKUP_SIZE);
}
