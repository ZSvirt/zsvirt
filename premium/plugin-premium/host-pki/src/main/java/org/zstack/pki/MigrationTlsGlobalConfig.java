package org.zstack.pki;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

@GlobalConfigDefinition
public class MigrationTlsGlobalConfig {
    public static final String CATEGORY = "kvm";

    @GlobalConfigValidation
    public static GlobalConfig MIGRATE_TLS_ENABLE = new GlobalConfig(CATEGORY, "migrate.tls.enable");

    @GlobalConfigValidation
    public static GlobalConfig MIGRATE_TLS_MUTUAL_AUTH = new GlobalConfig(CATEGORY, "migrate.tls.mutualAuth");

    @GlobalConfigValidation(validValues = {"strict", "permissive"})
    public static GlobalConfig MIGRATE_TLS_POLICY = new GlobalConfig(CATEGORY, "migrate.tls.policy");

    @GlobalConfigValidation
    public static GlobalConfig MIGRATE_TLS_MIN_VERSION = new GlobalConfig(CATEGORY, "migrate.tls.minVersion");
}
