package org.zstack.header.identity.login;

import org.zstack.header.errorcode.ErrorCode;

/**
 * Additional authentication extension point interface.
 *
 * This extension is used for additional authentications which do not
 * need known user information or do user verification but only offer
 * extra verification like captcha or verification code.
 */
public interface LoginAuthExtensionPoint {
    ErrorCode beforeExecuteLogin(LoginContext loginContext);

    ErrorCode postLogin(LoginContext loginContext, LoginSessionInfo info);

    void afterLoginSuccess(LoginContext loginContext, LoginSessionInfo info);

    void afterLoginFailure(LoginContext loginContext, LoginSessionInfo info, ErrorCode errorCode);

    AdditionalAuthFeature getAdditionalAuthFeature();

    LoginAuthenticationProcedureDesc getAdditionalAuthDesc(LoginContext loginContext);
}
