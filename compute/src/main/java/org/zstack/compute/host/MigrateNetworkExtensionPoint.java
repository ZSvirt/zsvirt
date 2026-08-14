package org.zstack.compute.host;

public interface MigrateNetworkExtensionPoint {
    class MigrateInfo {
        public String dstMigrationAddress;
        public String srcMigrationAddress;
    }

    MigrateInfo getMigrationAddressForVM(String srcHostUuid, String dstHostUuid);
}
