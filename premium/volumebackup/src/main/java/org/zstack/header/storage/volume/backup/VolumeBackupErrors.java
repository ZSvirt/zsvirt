package org.zstack.header.storage.volume.backup;

/**
 * Created by MaJin on 2019/5/24.
 */
public enum VolumeBackupErrors {
    VM_STOPPED(1000);

    private String code;

    VolumeBackupErrors(int id) {
        code = String.format("VOL_BACKUP.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }

}
