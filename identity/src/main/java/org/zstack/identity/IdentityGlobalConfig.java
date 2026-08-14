package org.zstack.identity;

import org.zstack.core.config.*;
import org.zstack.header.core.encrypt.GlobalConfigEncrypt;

/**
 */
@GlobalConfigDefinition
public class IdentityGlobalConfig {
    public static final String CATEGORY = "identity";

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig MAX_CONCURRENT_SESSION = new GlobalConfig(CATEGORY, "session.maxConcurrent");

    @GlobalConfigEncrypt(category = CATEGORY, name = "session.timeout")
    @GlobalConfigValidation(min = 0, max = 31536000)
    public static GlobalConfig SESSION_TIMEOUT = new GlobalConfig(CATEGORY, "session.timeout");
    @GlobalConfigValidation(min = 1)
    public static GlobalConfig SESSION_CLEANUP_INTERVAL = new GlobalConfig(CATEGORY, "session.cleanup.interval");
    @GlobalConfigValidation
    public static GlobalConfig SHOW_ALL_RESOURCE_TO_ADMIN = new GlobalConfig(CATEGORY, "admin.showAllResource");
    @GlobalConfigEncrypt(category = CATEGORY, name = "enable.unique.session")
    @GlobalConfigValidation(validValues = {"true", "false"})
    public static GlobalConfig ENABLE_UNIQUE_SESSION = new GlobalConfig(CATEGORY, "enable.unique.session");
}
