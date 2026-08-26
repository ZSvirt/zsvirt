package org.zstack.header.storage.volume.backup;

public interface VmBackupKvmCommandExtensionPoint {
    void beforeTakeVmBackup(VmBackupKvmCommandExtensionContext context);

    default void rollbackTakeVmBackup(VmBackupKvmCommandExtensionContext context) {
    }
}
