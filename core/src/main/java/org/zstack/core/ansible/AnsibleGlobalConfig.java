package org.zstack.core.ansible;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * Created by mingjian.deng on 2019/6/12.
 */
@GlobalConfigDefinition
public class AnsibleGlobalConfig {
    public static final String CATEGORY = "ansible";

    @GlobalConfigValidation
    public static GlobalConfig CHECK_MANAGEMENT_CALLBACK = new GlobalConfig(CATEGORY, "check.management.callback");

    @GlobalConfigValidation
    public static GlobalConfig ENABLE_ANSIBLE_CACHE_SYSTEM_INFO = new GlobalConfig(CATEGORY, "enable.ansible.cache.system.info");
}
