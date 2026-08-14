package org.zstack.identity.imports;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO;
import org.zstack.resourceconfig.BindResourceConfig;

/**
 * Created by Wenhao.Zhang on 2024/06/05
 */
@GlobalConfigDefinition
public class AccountImportsGlobalConfig {
    public static final String CATEGORY = "accountImport";

    @BindResourceConfig({ThirdPartyAccountSourceVO.class})
    @GlobalConfigDef(defaultValue = "false", description = "whether execute account synchronization task after system startup", type = Boolean.class)
    public static GlobalConfig SYNC_ACCOUNTS_ON_START = new GlobalConfig(CATEGORY, "sync.accounts.on.start");

    @GlobalConfigValidation(min = 1000)
    @BindResourceConfig({ThirdPartyAccountSourceVO.class})
    @GlobalConfigDef(defaultValue = "5000", description = "account source connection timeout in millis", type = Long.class)
    public static GlobalConfig SOURCE_CONNECT_TIMEOUT_MILLIS = new GlobalConfig(CATEGORY, "source.connect.timeout.millis");

    @GlobalConfigValidation(min = 1000)
    @BindResourceConfig({ThirdPartyAccountSourceVO.class})
    @GlobalConfigDef(defaultValue = "5000", description = "account source reading timeout in millis", type = Long.class)
    public static GlobalConfig SOURCE_READ_TIMEOUT_MILLIS = new GlobalConfig(CATEGORY, "source.read.timeout.millis");

    @BindResourceConfig({ThirdPartyAccountSourceVO.class})
    @GlobalConfigDef(defaultValue = "false", description = "enable automatic synchronization of account from remote server", type = Boolean.class)
    public static GlobalConfig AUTO_SYNC_ENABLE = new GlobalConfig(CATEGORY, "auto.sync.enable");

    @GlobalConfigValidation(min = 60)
    @BindResourceConfig({ThirdPartyAccountSourceVO.class})
    @GlobalConfigDef(defaultValue = "86400", description = "time interval in seconds for automatic synchronization of account from remote server", type = Long.class)
    public static GlobalConfig AUTO_SYNC_INTERVAL_SECONDS = new GlobalConfig(CATEGORY, "auto.sync.interval.seconds");
}
