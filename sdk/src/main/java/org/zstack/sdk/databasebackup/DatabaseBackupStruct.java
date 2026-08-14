package org.zstack.sdk.databasebackup;

import org.zstack.sdk.databasebackup.DatabaseType;

public class DatabaseBackupStruct  {

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public java.lang.String version;
    public void setVersion(java.lang.String version) {
        this.version = version;
    }
    public java.lang.String getVersion() {
        return this.version;
    }

    public java.lang.String installPath;
    public void setInstallPath(java.lang.String installPath) {
        this.installPath = installPath;
    }
    public java.lang.String getInstallPath() {
        return this.installPath;
    }

    public DatabaseType type;
    public void setType(DatabaseType type) {
        this.type = type;
    }
    public DatabaseType getType() {
        return this.type;
    }

    public java.lang.Long size;
    public void setSize(java.lang.Long size) {
        this.size = size;
    }
    public java.lang.Long getSize() {
        return this.size;
    }

    public java.lang.String md5;
    public void setMd5(java.lang.String md5) {
        this.md5 = md5;
    }
    public java.lang.String getMd5() {
        return this.md5;
    }

    public java.sql.Timestamp createdTime;
    public void setCreatedTime(java.sql.Timestamp createdTime) {
        this.createdTime = createdTime;
    }
    public java.sql.Timestamp getCreatedTime() {
        return this.createdTime;
    }

}
