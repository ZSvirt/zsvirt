package org.zstack.header.storage.backup;

import org.zstack.header.errorcode.ErrorCode;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 3:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class BackupStorageException extends Exception {
    private ErrorCode error;

    public BackupStorageException(ErrorCode code) {
        this.error = code;
    }

    public BackupStorageException(String message) {
        super(message);
    }

    public BackupStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public BackupStorageException(Throwable cause) {
        super(cause);
    }

    public ErrorCode getError() {
        return error;
    }

    public void setError(ErrorCode error) {
        this.error = error;
    }
}
