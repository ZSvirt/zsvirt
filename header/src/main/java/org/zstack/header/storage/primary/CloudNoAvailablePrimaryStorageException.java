package org.zstack.header.storage.primary;

public class CloudNoAvailablePrimaryStorageException extends Exception {
    public CloudNoAvailablePrimaryStorageException(String msg) {
        super(msg);
    }

    public CloudNoAvailablePrimaryStorageException(String msg, Throwable t) {
        super(msg, t);
    }
}
