package org.zstack.storage.backup;

import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.mevoco.PremiumGlobalConfig;

@GlobalConfigDefinition
public class DatabaseBackupGlobalConfig {
    public static final String CATEGORY = "databaseBackup";

    @GlobalConfigValidation
    public static PremiumGlobalConfig ALLOW_COVER_DATABASE = new PremiumGlobalConfig(CATEGORY, "coverDatabase.allow");
}
