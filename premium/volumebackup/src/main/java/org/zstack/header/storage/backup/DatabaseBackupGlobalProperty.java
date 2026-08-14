package org.zstack.header.storage.backup;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Created by MaJin on 2019/12/23.
 */
@GlobalPropertyDefinition
public class DatabaseBackupGlobalProperty {
    @GlobalProperty(name="upgradeDatabaseBackupHistory", defaultValue = "false")
    public static boolean UPGRADE_DATABASE_BACKUP_HISTORY;
}
