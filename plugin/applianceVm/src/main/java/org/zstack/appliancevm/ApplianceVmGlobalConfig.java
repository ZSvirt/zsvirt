package org.zstack.appliancevm;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 */
@GlobalConfigDefinition
public class ApplianceVmGlobalConfig {
    public static final String CATEGORY = "applianceVm";

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig CONNECT_TIMEOUT = new GlobalConfig(CATEGORY, "connect.timeout");
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig SSH_LOGIN_TIMEOUT = new GlobalConfig(CATEGORY, "ssh.timeout");
    @GlobalConfigValidation
    public static GlobalConfig DEPLOY_AGENT_ON_START = new GlobalConfig(CATEGORY, "agent.deployOnStart");
    @GlobalConfigValidation(min = 1)
    public static GlobalConfig BOOTSTRAPINFO_TIMEOUT = new GlobalConfig(CATEGORY, "bootstrapinfo.timeout");

    @GlobalConfigValidation(min = 300)
    public static GlobalConfig DELETE_TIMEOUT = new GlobalConfig(CATEGORY, "deletion.timeout");
    @GlobalConfigValidation
    public static GlobalConfig APPLIANCENUMA = new GlobalConfig(CATEGORY, "applianceVmNuma");

    @GlobalConfigValidation
    public static GlobalConfig AUTO_ROLLBACK = new GlobalConfig(CATEGORY, "auto.rollback");

    @GlobalConfigValidation()
    public static GlobalConfig ENABLE_ABNORMAL_FILE_REPORTER = new GlobalConfig(CATEGORY, "enableAbnormalFileReporter");

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig ABNORMAL_FILE_MAX_SIZE = new GlobalConfig(CATEGORY, "abnormalFileMaxSize");
}
