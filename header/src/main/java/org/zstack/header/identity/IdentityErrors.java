package org.zstack.header.identity;

/**
 */
public enum IdentityErrors {
    AUTHENTICATION_ERROR(1000),
    INVALID_SESSION(1001),
    PERMISSION_DENIED(1002),
    QUOTA_EXCEEDING(1003),
    QUOTA_INVALID_OP(1004),
    NEED_CHANGE_PASSWORD(1005),
    MAX_CONCURRENT_SESSION_EXCEEDED(1006),
    ACCOUNT_DISABLED(1007),

    ADDITION_AUTHENTICATION_ERROR(2000),
    NEED_ADDITION_AUTHENTICATION(2001),
    ADDITION_AUTHENTICATION_FAIL(2002),
    ADDITION_AUTHENTICATION_SERVER_ERROR(2003),

    INVALID_ROLE_POLICY(3001),
    ROLE_BEING_USED(3002),
    ;

    private String code;

    private IdentityErrors(int id) {
        code = String.format("ID.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
