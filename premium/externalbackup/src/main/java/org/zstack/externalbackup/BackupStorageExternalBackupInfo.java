package org.zstack.externalbackup;

public class BackupStorageExternalBackupInfo extends ResourceExternalBackupInfo {
    protected long size;

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
