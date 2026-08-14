package org.zstack.storage.backup.sftp;

public class SftpBackupStorageException extends Exception {
    public SftpBackupStorageException(String msg) {
        super(msg);
    }
    
    public SftpBackupStorageException(String msg, Throwable t) {
        super(msg, t);
    }
    
    public SftpBackupStorageException(Throwable t) {
        super(t);
    }
}
