package org.zstack.core.captcha;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * Created by kayo on 2018/8/7.
 */
@GlobalConfigDefinition
public class CaptchaGlobalConfig {
    public static final String CATEGORY = "captcha";

    @GlobalConfigValidation(min = 1)
    @GlobalConfigDef(defaultValue = "3600", type = Long.class, description = "The interval management server checks expired captcha, in seconds")
    public static GlobalConfig CAPTCHA_CLEANUP_INTERVAL = new GlobalConfig(CATEGORY, "captcha.cleanup.interval");

    @GlobalConfigValidation(min = 1)
    @GlobalConfigDef(defaultValue = "300", type = Long.class, description = "Period of validity for a verify code, in minutes")
    public static GlobalConfig CAPTCHA_VALID_PERIOD = new GlobalConfig(CATEGORY, "captcha.valid.period");

}
