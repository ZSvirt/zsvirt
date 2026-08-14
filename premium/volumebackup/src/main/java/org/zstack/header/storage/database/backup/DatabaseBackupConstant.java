package org.zstack.header.storage.database.backup;

import org.zstack.header.storage.backup.BackupConstant;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DatabaseBackupConstant {
    public static final String SERVICE_ID = "backup.database";
    public static final String ACTION_CATEGORY = "databaseBackup";
    public static final String imageBackupName = "zsbak";

    public static String buildDatabaseBackupName(String dbVersion){
        return String.format("%s-%s-%s.gz", BackupConstant.DB_BACKUP_PREFIX, dbVersion,
                new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance().getTime()));
    }
}
