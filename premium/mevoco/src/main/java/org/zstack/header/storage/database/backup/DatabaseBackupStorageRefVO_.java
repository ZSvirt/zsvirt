package org.zstack.header.storage.database.backup;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(DatabaseBackupStorageRefVO.class)
public class DatabaseBackupStorageRefVO_ {
    public static volatile SingularAttribute<DatabaseBackupStorageRefVO, Long> id;
    public static volatile SingularAttribute<DatabaseBackupStorageRefVO, String> databaseBackupUuid;
    public static volatile SingularAttribute<DatabaseBackupStorageRefVO, String> backupStorageUuid;
    public static volatile SingularAttribute<DatabaseBackupStorageRefVO, String> installPath;
    public static volatile SingularAttribute<DatabaseBackupStorageRefVO, String> exportUrl;
    public static volatile SingularAttribute<DatabaseBackupStorageRefVO, DatabaseBackupStatus> status;
    public static volatile SingularAttribute<DatabaseBackupStorageRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<DatabaseBackupStorageRefVO, Timestamp> lastOpDate;
}
