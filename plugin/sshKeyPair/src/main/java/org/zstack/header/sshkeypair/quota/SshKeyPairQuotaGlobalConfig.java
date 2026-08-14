package org.zstack.header.sshkeypair.quota;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.identity.QuotaGlobalConfig;

@GlobalConfigDefinition
public class SshKeyPairQuotaGlobalConfig extends QuotaGlobalConfig {

    @GlobalConfigValidation(min = 0)
    @GlobalConfigDef(type = Integer.class, defaultValue = "20", description = "default ssh key pair quota")
    public static GlobalConfig SSH_KEY_PAIR_NUM = new GlobalConfig(CATEGORY, SshKeyPairQuotaConstant.SSH_KEY_PAIR_NUM);
}
