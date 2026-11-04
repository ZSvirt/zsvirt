package org.zstack.twoFactorAuthentication;

import org.zstack.header.identity.AccountConstant;

public interface TwoFactorAuthenticationConstant {
    String SERVICE_ID = "twoFactorAuthentication";

    String ACTION_CATEGORY = "twoFactorAuthentication";

    /**
     * keep back compatibility and equals to account
     */
    String TWO_FACTOR_ACCOUNT_LOGIN_TYPE = "twoFactorAccount";

    String GOOGLE_AUTHENTICATION_TYPE = "googleAuth";

    String AUTHENTICATION_SOURCE = "authenticationSource";

    String LOGIN_TYPE_ACCOUNT = AccountConstant.LOGIN_TYPE;

    String LOGIN_TYPE_LDAP = "ldap";
}
