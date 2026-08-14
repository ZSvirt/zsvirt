package org.zstack.storage.backup.sftp;

/**
 */
public enum SftpBackupStorageErrors {
    RECONNECT_ERROR(1000);

    private String code;

    private SftpBackupStorageErrors(int id) {
        code = String.format("BS.SFTP.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
