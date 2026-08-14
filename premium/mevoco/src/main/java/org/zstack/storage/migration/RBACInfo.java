package org.zstack.storage.migration;

import org.zstack.header.description.PackageDescription;
import org.zstack.storage.migration.backup.APIBackupStorageMigrateImageMsg;
import org.zstack.storage.migration.backup.APIGetBackupStorageCandidatesForImageMigrationMsg;
import org.zstack.storage.migration.primary.APIGetHostCandidatesForVmMigrationMsg;
import org.zstack.storage.migration.primary.APIGetPrimaryStorageCandidatesForVmMigrationMsg;
import org.zstack.storage.migration.primary.APIGetPrimaryStorageCandidatesForVolumeMigrationMsg;
import org.zstack.storage.migration.primary.APIPrimaryStorageMigrateVmMsg;
import org.zstack.storage.migration.primary.APIPrimaryStorageMigrateVolumeMsg;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "storage-migration";
    }

    {
        permissionBuilder()
                .communityAvailable()
                .zsvProAvailable()
                .build();

        roleContributorBuilder()
                .roleName("vm")
                .actions(
                        APIGetHostCandidatesForVmMigrationMsg.class,
                        APIGetPrimaryStorageCandidatesForVmMigrationMsg.class,
                        APIPrimaryStorageMigrateVmMsg.class)
                .build();

        roleContributorBuilder()
                .roleName("volume")
                .actions(
                        APIGetPrimaryStorageCandidatesForVolumeMigrationMsg.class,
                        APIPrimaryStorageMigrateVolumeMsg.class)
                .build();

        roleContributorBuilder()
                .roleName("image")
                .actions(
                        APIGetBackupStorageCandidatesForImageMigrationMsg.class,
                        APIBackupStorageMigrateImageMsg.class)
                .build();

        apis()
                .inPackage("org.zstack.storage.migration.backup")
                .toService("mevoco")
                .build();
        apis()
                .inPackage("org.zstack.storage.migration.primary")
                .toService("mevoco")
                .build();
    }
}
