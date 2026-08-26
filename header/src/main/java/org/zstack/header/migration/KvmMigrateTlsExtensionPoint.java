package org.zstack.header.migration;

/**
 * Premium/product layer configures TLS fields on {@code MigrateVmCmd} before kvmagent is called.
 */
public interface KvmMigrateTlsExtensionPoint {
    /**
     * @return true if any extension handled TLS configuration for this migration
     */
    boolean configureMigrateTls(String vmUuid, String srcHostUuid, String dstHostUuid,
                                String dstMigrationAddress, KvmMigrateTlsSpec spec);
}
