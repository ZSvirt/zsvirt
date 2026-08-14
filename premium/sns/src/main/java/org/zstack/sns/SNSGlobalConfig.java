package org.zstack.sns;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

@GlobalConfigDefinition
public class SNSGlobalConfig {
    public static final String CATEGORY = "sns";

    @GlobalConfigValidation(min = 1)
    @GlobalConfigDef(defaultValue = "5", type = Long.class, description = "endpoint ")
    public static GlobalConfig SNS_APPLICATION_ENDPOINT_CONNECTION_STATUS_INTERVAL  = new GlobalConfig(CATEGORY, "endpoint.connectionStatusCheckInterval");
}
