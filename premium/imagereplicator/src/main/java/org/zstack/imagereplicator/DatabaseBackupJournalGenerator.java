package org.zstack.imagereplicator;

import org.zstack.header.storage.database.backup.DatabaseBackupInventory;

public interface DatabaseBackupJournalGenerator {
    void generateInitialRecords(String bsUuid);
    void onUpdateDatabaseBackup(DatabaseBackupInventory inv);
    void onAddDatabaseBackup(DatabaseBackupInventory inv);
    void onExpungeDatabaseBackup(String databaseBackupUuid, String bsUuid);
}
