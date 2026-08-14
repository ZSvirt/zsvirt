package org.zstack.header.host;

/**
 */
public enum HostErrors {
    UNABLE_TO_ADD_HOST(1000),
    UNABLE_TO_ENTER_MAINTENANCE_MODE(1001),
    UNABLE_TO_RECONNECT_HOST(1002),
    CONNECTION_ERROR(1003),
    ALLOCATE_HOST_ERROR(1004),
    FAILED_TO_START_VM_ON_HYPERVISOR(1005),
    FAILED_TO_STOP_VM_ON_HYPERVISOR(1006),
    FAILED_TO_REBOOT_VM_ON_HYPERVISOR(1007),
    FAILED_TO_DESTROY_VM_ON_HYPERVISOR(1008),
    FAILED_TO_MIGRATE_VM_ON_HYPERVISOR(1009),
    HOST_IS_DISCONNECTED(1010),
    OPERATION_FAILURE_GC_ELIGIBLE(1011),
    HOST_PASSWORD_HAS_BEEN_CHANGED(1012),
    ;
    private String code;

    private HostErrors(int id) {
        code = String.format("HOST.%s", id);
    }

    public boolean isEqual(Object t) {
        return t.toString().equals(this.toString());
    }

    @Override
    public String toString() {
        return code;
    }

    public enum Opaque {
        NO_RECONNECT_AFTER_PING_FAILURE;
    }
}
