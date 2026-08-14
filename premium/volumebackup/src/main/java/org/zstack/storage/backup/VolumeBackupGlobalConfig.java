package org.zstack.storage.backup;

import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.mevoco.PremiumGlobalConfig;

@GlobalConfigDefinition
public class VolumeBackupGlobalConfig {
    public static final String CATEGORY = "volumeBackup";

    @GlobalConfigValidation(inNumberRange = {1, 64})
    public static PremiumGlobalConfig MAX_INCREMENTAL_BACKUP = new PremiumGlobalConfig(CATEGORY, "incrementalBackup.maxNum");

    @GlobalConfigValidation()
    public static PremiumGlobalConfig REBACKUP_STOPPED_VM = new PremiumGlobalConfig(CATEGORY, "rebackup.stoppedVm");

}
