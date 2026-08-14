package org.zstack.header.storage.primary;

public class CloudUnableToReservePrimaryStorageCapacityException extends Exception {
    public CloudUnableToReservePrimaryStorageCapacityException(String msg) {
        super(msg);
    }

    public CloudUnableToReservePrimaryStorageCapacityException(String msg, Throwable t) {
        super(msg, t);
    }
}
