package org.zstack.drs.header;

public enum DRSErrors {
    GENERAL_ERROR(1000),

    DRS_ALREADY_EXISTS(1001),
    DRS_DISABLED(1002),
    DRS_NOT_SUPPORT(1003),
    DRS_EXECUTE_FAILURE(1004),
    DRS_ADVICE_NOT_PREPARED(1005),
    ;

    private String code;

    private DRSErrors(int id) {
        code = String.format("DRS.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
