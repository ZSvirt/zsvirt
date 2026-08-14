package org.zstack.header.storage.addon.backup;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.storage.backup.BackupStorageVO;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "external-backup-storage";
    }

    {
        permissionBuilder()
                .adminOnlyAPIs("org.zstack.header.storage.addon.backup.**")
                .communityAvailable()
                .build();

        globalReadableResourceBuilder()
                .resources(BackupStorageVO.class)
                .build();

        apis()
                .inThisPackage()
                .toService("storage.backup")
                .build();
    }
}
