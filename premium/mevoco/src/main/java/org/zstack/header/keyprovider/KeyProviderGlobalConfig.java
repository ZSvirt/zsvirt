package org.zstack.header.keyprovider;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

@GlobalConfigDefinition
public class KeyProviderGlobalConfig {
    public static final String CATEGORY = "keyProvider";
    public static final String NONE = "None";

    @GlobalConfigValidation
    @GlobalConfigDef(defaultValue = NONE, description = "default key provider uuid")
    public static GlobalConfig DEFAULT_KEY_PROVIDER_UUID = new GlobalConfig(CATEGORY, "default.keyProviderUuid");

    @GlobalConfigValidation(min = 0, max = 65536)
    @GlobalConfigDef(defaultValue = "50051", description = "key-tool grpc port")
    public static GlobalConfig KEY_TOOL_GRPC_PORT = new GlobalConfig(CATEGORY, "keyTool.grpcPort");
}
