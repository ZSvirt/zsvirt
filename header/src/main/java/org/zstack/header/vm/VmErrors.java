package org.zstack.header.vm;

/**
 */
public enum VmErrors {
    CREATE_ERROR(1000),
    START_ERROR(1001),
    STOP_ERROR(1002),
    REBOOT_ERROR(1003),
    DESTROY_ERROR(1004),
    ATTACH_VOLUME_ERROR(1005),
    DETACH_VOLUME_ERROR(1006),
    ATTACH_NETWORK_ERROR(1007),
    MIGRATE_ERROR(1008),
    ALLOCATE_MAC_ERROR(1009),
    NOT_IN_CORRECT_STATE(1010),
    CHANGE_VM_PASSWORD_ERROR(1011),
    SUSPEND_ERROR(1012),
    RESUME_ERROR(1013),
    RE_IMAGE_VM_NOT_IN_STOPPED_STATE(1014),
    RE_IMAGE_IMAGE_MEDIA_TYPE_SHOULD_NOT_BE_ISO(1015),
    RE_IMAGE_CANNOT_FIND_IMAGE_CACHE(1016),
    VNC_SETTING_ERROR(1017),

    // Errors in VM allocation
    VM_ALLOCATION_ERROR(1200),
    WRONG_SPECIFIC_HOST_ERROR(1201),
    WRONG_SPECIFIC_PS_ERROR(1202),
    ;

    private String code;

    private VmErrors(int id) {
        code = String.format("VM.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
