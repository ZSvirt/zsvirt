package org.zstack.header.identity.login;

public interface LoginAuthConstant {
    String basicLoginControlAuth = "BASIC_LOGIN_CONTROL_AUTH";
    AdditionalAuthFeature basicLoginControl = new AdditionalAuthFeature(basicLoginControlAuth);

    String twoFactorAuth = "TWO_FACTOR_AUTH";
    AdditionalAuthFeature twoFactor = new AdditionalAuthFeature(twoFactorAuth);

    String LOGIN_SESSION_INVENTORY = "LOGIN_SESSION_INVENTORY";
    String LOGIN_SESSION_INFO = "LOGIN_SESSION_INFO";
}
