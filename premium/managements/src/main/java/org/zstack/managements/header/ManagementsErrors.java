package org.zstack.managements.header;

public enum ManagementsErrors {
    GENERAL_ERROR(1000),

    // zstack HA2 errors (multi-management-node error)
    HA2_ERROR(2000),
    MISSING_HA2_TOOLS(2001),

    HA2_STATUS_GET_ERROR(2101), // zsha2 status info error: 2101 ~ 2109
    HA2_STATUS_PARSE_ERROR(2102),
    HA2_NOT_INSTALLED(2103),
    ;

    private String code;

    private ManagementsErrors(int id) {
        code = String.format("MANAGEMENTS.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
