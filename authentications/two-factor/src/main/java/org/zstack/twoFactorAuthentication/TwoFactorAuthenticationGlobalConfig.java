package org.zstack.twoFactorAuthentication;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.header.core.encrypt.GlobalConfigEncrypt;

/**
 */
@GlobalConfigDefinition
public class TwoFactorAuthenticationGlobalConfig {
    public static final String CATEGORY = "twofa";

    @GlobalConfigEncrypt(category = CATEGORY, name = "twofa.enable")
    public static GlobalConfig ENABLE_TWOFA_AUTH = new GlobalConfig(CATEGORY, "twofa.enable");
}
