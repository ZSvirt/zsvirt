package org.zstack.header.storage.database.backup;


import java.sql.Timestamp;

public class DatabaseBackupMetadata {
    public String name;

    public String description;

    public String version;

    public DatabaseType type;

    public Timestamp createdTime;

    public String md5;
}
