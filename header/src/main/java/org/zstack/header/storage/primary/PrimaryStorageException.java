package org.zstack.header.storage.primary;

public class PrimaryStorageException extends Exception {
    public PrimaryStorageException(String msg) {
        super(msg);
    }

    public PrimaryStorageException(String msg, Throwable t) {
        super(msg, t);
    }
}
