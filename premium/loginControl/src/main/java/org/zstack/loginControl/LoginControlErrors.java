package org.zstack.loginControl;

public enum LoginControlErrors {
    FORCE_CHANGE_PASSWORD_ERROR(1000),
    HISTORICAL_PASSWORD_REPEATED_ERROR(1001),
    IDENTITY_LOCKED_ERROR(1002),
    CAPTCHA_ERROR(1003),
    REQUEST_REFUSED_ERROR(1004),
    CAPTCHA_MISSING(1005);

    private String code;

    private LoginControlErrors(int id) {
        code = String.format("LOGIN_CONTROL.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
