package org.zstack.twoFactorAuthentication;

public interface TwoFactorAuthenticationFactory {
    String getType();

    TwoFactorAuthenticationStruct createAuthentication(TwoFactorAuthenticationParamStruct msg);

    String getLoginType();
}
