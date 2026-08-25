package org.zstack.pki;

import org.zstack.header.migration.MigrationTlsConstant;

public class MigrationTlsPolicy {
    public boolean isMutualTls() {
        return MigrationTlsGlobalConfig.MIGRATE_TLS_MUTUAL_AUTH.value(Boolean.class);
    }

    public boolean isStrict() {
        return MigrationTlsConstant.POLICY_STRICT.equals(
                MigrationTlsGlobalConfig.MIGRATE_TLS_POLICY.value(String.class));
    }
}
