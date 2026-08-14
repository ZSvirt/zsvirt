package org.zstack.header.allocator;

public class UnableToReserveHostCapacityException extends RuntimeException {
    public UnableToReserveHostCapacityException(String msg) {
        super(msg);
    }
}
