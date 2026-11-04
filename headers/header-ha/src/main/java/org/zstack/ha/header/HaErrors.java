package org.zstack.ha.header;

public enum HaErrors {
    GENERAL_ERROR(1000),

    // 2XXX: HA configuration
    CONFIG_ERROR(2000),
    INVALID_HA_LEVEL(2001),
    INVALID_CHECK_SUCCESS_RATIO(2002),

    // 21XX: configuration update blocked by host state
    CONFIG_UPDATE_BLOCKED(2100),
    DISCONNECTED_HOST_HAS_UNKNOWN_VM(2101),

    // 3XXX: self-fencer deployment
    FENCER_DEPLOY_ERROR(3000),
    SETUP_SELF_FENCER_FAILED(3001),
    CANCEL_SELF_FENCER_FAILED(3002),
    SYNC_SELF_FENCER_STATE_FAILED(3003),
    FENCER_SYNC_TASK_CONFLICT(3004),

    // 4XXX: judging whether the VM host is dead
    VM_FENCE_ERROR(4000),
    NO_HA_HOST_CHECKER(4001),
    VM_HOST_NOT_FOUND(4002),
    HOST_SCAN_FAILED(4003),
    MANAGEMENT_NODE_SCAN_FAILED(4004),

    // 41XX: storage fencer verdict
    STORAGE_FENCE_ERROR(4100),
    PRIMARY_STORAGE_HOST_DISCONNECTED(4101),
    HEARTBEAT_NOT_UPDATED(4102),
    HOST_ID_NOT_FOUND(4103),

    // 5XXX: starting the VM again after HA
    VM_HA_START_ERROR(5000),
    HA_WORKER_ALREADY_STARTED(5001),

    // 51XX: HA start refused (code is equals to VmErrors)
    VM_HA_START_BLOCKED(5100),
    PRIMARY_STORAGE_IN_MAINTENANCE(5101),
    LV_LOCKED_BY_OTHER_HOST(5102), // LV locked by other host
    ;

    public final String code;

    HaErrors(int id) {
        code = String.format("HA.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
