package org.zstack.zmigrate;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

@GlobalConfigDefinition
public class ZMigrateGlobalConfig {
    public static final String CATEGORY = "zmigrate";

    @GlobalConfigValidation
    @GlobalConfigDef(defaultValue = "dGtiUVlTTXNCZmZySzRlcnhXTmI=", description = "base64-encoded password of ssh user on gateway")
    public static GlobalConfig GATEWAY_SSH_PASSWORD = new GlobalConfig(CATEGORY, "gateway.ssh.password");

    @GlobalConfigValidation
    @GlobalConfigDef(defaultValue = "fefafde4d31734dfbe63b36c8093e022", description = "uuid of platform account")
    public static GlobalConfig PLATFORM_ACCOUNT_UUID = new GlobalConfig(CATEGORY, "platform.account.uuid");

    @GlobalConfigValidation
    @GlobalConfigDef(defaultValue = "f2642ab77ea33ca59509be99fd91abda", description = "uuid of platform region")
    public static GlobalConfig PLATFORM_REGION_UUID = new GlobalConfig(CATEGORY, "platform.region.uuid");

    @GlobalConfigValidation(min = 0)
    @GlobalConfigDef(defaultValue = "8053063680", description = "The upper limit of the estimated total size of all zmigrate images, in bytes", type = Long.class)
    public static GlobalConfig IMAGE_ESTIMATED_TOTAL_SIZE = new GlobalConfig(CATEGORY, "image.estimated.total.size");

    @GlobalConfigValidation
    @GlobalConfigDef(defaultValue = "6.7.3-6d28fff82e478958e5215d14a7ec8573;8.0.3-007ab979e52f52401f02278b75ab5c74",
            description = "required ZMigrate VDDK versions and MD5 checksums", type = String.class)
    public static GlobalConfig VDDK_VERSION_MD5 = new GlobalConfig(CATEGORY, "vddk.version.md5");
}
