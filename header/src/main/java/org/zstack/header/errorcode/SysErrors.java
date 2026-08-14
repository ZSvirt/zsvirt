package org.zstack.header.errorcode;

public enum SysErrors {
    INTERNAL(1000),
    TIMEOUT(1001),
    CREATE_RESOURCE_ERROR(1002),
    RESOURCE_NOT_FOUND(1003),
    DELETE_RESOURCE_ERROR(1004),
    CHANGE_RESOURCE_STATE_ERROR(1005),
    OPERATION_ERROR(1006),
    INVALID_ARGUMENT_ERROR(1007),
    UNKNOWN_MESSAGE_ERROR(1008),
    NO_ROUTE_ERROR(1009),
    NOT_READY_ERROR(1010),
    UNDELIVERABLE_ERROR(1011),
    MANAGEMENT_NODE_UNAVAILABLE_ERROR(1012),
    NO_CAPABILITY_ERROR(1013),
    UNIMPLEMENTED_OPERATION_ERROR(1014),
    HTTP_ERROR(1015),
    IO_ERROR(1016),
    CANCEL_ERROR(1017),
    RESOURCE_NOT_ACCESSIBLE(1018),
    OPERATION_DENIED(1019),

    // ZSphere only
    MULTIPLE_REASONS(1090),
    NOT_SUPPORTED(1091),

    // CloudBus related Error - ZSphere only - 211X
    CLOUD_BUS_SEND_ERROR(2111),
    ;

    private String code;

    SysErrors(int id) {
        code = String.format("SYS.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }

}
