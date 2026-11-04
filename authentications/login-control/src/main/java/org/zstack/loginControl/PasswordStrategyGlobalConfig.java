package org.zstack.loginControl;

import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.core.encrypt.GlobalConfigEncrypt;
import org.zstack.core.config.GlobalConfig;

@GlobalConfigDefinition
public class PasswordStrategyGlobalConfig {
    public static final String CATEGORY = "passwordStrategy";

    @GlobalConfigEncrypt(category = CATEGORY, name = "enable.lock.login.attempts.maximum")
    @GlobalConfigDef(defaultValue = "false", type = Boolean.class, description = "Enable lock login fail accounts")
    public static GlobalConfig ENABLE_LOCK_LOGIN_ATTEMPTS_MAXIMUM = new GlobalConfig(CATEGORY, "enable.lock.login.attempts.maximum");

    @GlobalConfigValidation(min = 0)
    @GlobalConfigDef(defaultValue = "6", type = Long.class, description = "The maximum to lock login fail accounts")
    public static GlobalConfig LOCK_LOGIN_ATTEMPTS_MAXIMUM = new GlobalConfig(CATEGORY, "lock.login.attempts.maximum");

    @GlobalConfigValidation(min = 0)
    @GlobalConfigDef(defaultValue = "600", type = Long.class, description = "The time period, deny account login operation")
    public static GlobalConfig LOCK_LOGIN_PERIOD = new GlobalConfig(CATEGORY, "lock.login.period");

    @GlobalConfigEncrypt(category = CATEGORY, name = "enable.force.change.password.period")
    @GlobalConfigDef(defaultValue = "false", type = Boolean.class, description = "Enable set period to force change password of current user")
    public static GlobalConfig ENABLE_FORCE_CHANGE_PASSWORD_PERIOD = new GlobalConfig(CATEGORY, "enable.force.change.password.period");

    @GlobalConfigEncrypt(category = CATEGORY, name = "force.change.password.period")
    @GlobalConfigValidation(min = 0)
    @GlobalConfigDef(defaultValue = "7776000", type = Long.class, description = "The period to force change password of current user")
    public static GlobalConfig FORCE_CHANGE_PASSWORD_PERIOD = new GlobalConfig(CATEGORY, "force.change.password.period");

    @GlobalConfigEncrypt(category = CATEGORY, name = "historical.password.num")
    @GlobalConfigValidation(min = 0)
    @GlobalConfigDef(defaultValue = "5", type = Integer.class, description = "Maximum password histories to record")
    public static GlobalConfig HISTORICAL_PASSWORD_NUM = new GlobalConfig(CATEGORY, "historical.password.num");

    @GlobalConfigEncrypt(category = CATEGORY, name = "enable.historical.password.compare")
    @GlobalConfigDef(defaultValue = "false", type = Boolean.class, description = "Enable historical password compare")
    public static GlobalConfig ENABLE_HISTORICAL_PASSWORD_COMPARE = new GlobalConfig(CATEGORY, "enable.historical.password.compare");

    @GlobalConfigEncrypt(category = CATEGORY, name = "password.strength.check.config")
    @GlobalConfigDef(defaultValue = "{\"enabled\":false,\"minimum\":8,\"maximum\":32,\"checkUppercase\":true,\"checkLowercase\":true,\"checkNumber\":true,\"checkSpecialWords\":true}",
            description = "Basic password strength check config")
    @GlobalConfigValidation(notEmpty = false)
    public static GlobalConfig PASSWORD_STRENGTH_CHECK_CONFIG = new GlobalConfig(CATEGORY, "password.strength.check.config");

}
