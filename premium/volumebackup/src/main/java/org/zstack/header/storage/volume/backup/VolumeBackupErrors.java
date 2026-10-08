package org.zstack.header.storage.volume.backup;

/**
 * Created by MaJin on 2019/5/24.
 */
public enum VolumeBackupErrors {
    VM_STOPPED(1000),
    ROOT_BACKUP_NOT_FOUND(1001);

    private String code;

    VolumeBackupErrors(int id) {
        code = String.format("VOL_BACKUP.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }

}
