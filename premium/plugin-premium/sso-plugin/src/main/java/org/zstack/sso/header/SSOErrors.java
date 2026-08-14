package org.zstack.sso.header;

/**
 * @Author: DaoDao
 * @Date: 2023/2/6
 */
public enum SSOErrors {
    INVALID_SSO_TOKEN(1001),

    SSO_ACCOUNT_NOT_FOUND(2001),
    ;

    private String code;

    private SSOErrors(int id) {
        code = String.format("SSO.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
