package org.zstack.header.storage.database.backup;

public interface DatabaseBackupVersionExtensionPoint {
    String getVersion(String schemaVersion);
    String getDeployMode();
}
