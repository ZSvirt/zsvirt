package org.zstack.loginControl;

import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.core.encrypt.GlobalConfigEncrypt;
import org.zstack.mevoco.PremiumGlobalConfig;

/**
 * Created by kayo on 2018/8/8.
 */
@GlobalConfigDefinition
public class LoginControlGlobalConfig {
    public static final String CATEGORY = "loginControl";

    @GlobalConfigEncrypt(category = CATEGORY, name = "login.control")
    @GlobalConfigValidation(validValues = {"true", "false"})
    @GlobalConfigDef(defaultValue = "false", type = Boolean.class, description = "Enable login control, including " +
            " login attempts check and verify code generation")
    public static PremiumGlobalConfig LOGIN_CONTROL = new PremiumGlobalConfig(CATEGORY, "login.control");

    @GlobalConfigEncrypt(category = CATEGORY, name = "login.attempts.maximum")
    @GlobalConfigValidation(min = 0)
    @GlobalConfigDef(defaultValue = "6", type = Long.class, description = "The maximum value to tolerate login failures " +
            "verify code will be required after that")
    public static PremiumGlobalConfig LOGIN_ATTEMPTS_MAXIMUM = new PremiumGlobalConfig(CATEGORY, "login.attempts.maximum");
}
