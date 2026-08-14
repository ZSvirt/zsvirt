package org.zstack.guesttools.header;

public enum GuestToolsErrors {
    GENERAL_ERROR(1000),
    INSTALLATION_ERROR(1001),
    DETACHED_ERROR(1002),
    FAILED_TO_GET_VERSION(1003),
    FAILED_TO_GET_STATUS(1004),
    NO_GUEST_TOOLS_FILES(1005),
    ;

    private String code;

    private GuestToolsErrors(int id) {
        code = String.format("GT.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
