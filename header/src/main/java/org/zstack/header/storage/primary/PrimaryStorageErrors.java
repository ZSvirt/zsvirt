package org.zstack.header.storage.primary;

/**
 */
public enum PrimaryStorageErrors {
    ALLOCATE_ERROR(1000),
    ATTACH_ERROR(1001),
    DETACH_ERROR(1002),
    DISCONNECTED(1003),
    FORBIDDENQOS(1004),
    TAKEN_OVER(1005);

    private String code;

    private PrimaryStorageErrors(int id) {
        code = String.format("PS.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
