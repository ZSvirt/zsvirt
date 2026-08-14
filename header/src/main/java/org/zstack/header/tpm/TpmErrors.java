package org.zstack.header.tpm;

public enum TpmErrors {
    GENERAL_ERROR(1000),

    // INVALID_ARGUMENT, 17xx <- SYS.1007
    TPM_ALREADY_EXISTS(1701),
    TPM_NOT_FOUND(1702),
    VM_STATE_ERROR(1703),
    ;

    private String code;

    private TpmErrors(int id) {
        code = String.format("TPM.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
