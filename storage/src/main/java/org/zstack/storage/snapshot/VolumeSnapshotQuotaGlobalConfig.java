package org.zstack.storage.snapshot;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.identity.QuotaGlobalConfig;

/**
 * Created by kayo on 2018/4/21.
 */
@GlobalConfigDefinition
public class VolumeSnapshotQuotaGlobalConfig extends QuotaGlobalConfig {

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig VOLUME_SNAPSHOT_NUM = new GlobalConfig(CATEGORY, VolumeSnapshotQuotaConstant.VOLUME_SNAPSHOT_NUM);
}
