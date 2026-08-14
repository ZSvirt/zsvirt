package org.zstack.core.timeout;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Created by david on 8/24/16.
 */
@GlobalPropertyDefinition
@Deprecated
public class MevocoApiGlobalTimeout {
    @GlobalProperty(name="ApiTimeout.org.zstack.header.vm.APICloneVmInstanceMsg", defaultValue = "timeout::36h")
    public static String APICloneVmInstanceMsg;

    @GlobalProperty(name="ApiTimeout.org.zstack.header.storage.backup.APIExportImageFromBackupStorageMsg", defaultValue = "timeout::3h")
    public static String APIExportImageFromBackupStorageMsg;

    @GlobalProperty(name="ApiTimeout.org.zstack.storage.backup.imagestore.APISyncImageFromImageStoreBackupStorageMsg", defaultValue = "timeout::72h")
    public static String APISyncImageFromImageStoreBackupStorageMsg;
    @GlobalProperty(name="ApiTimeout.org.zstack.storage.backup.imagestore.APIRecoveryImageFromImageStoreBackupStorageMsg", defaultValue = "timeout::72h")
    public static String APIRecoveryImageFromImageStoreBackupStorageMsg;

    @GlobalProperty(name="ApiTimeout.org.zstack.storage.migration.primary.APIPrimaryStorageMigrateVolumeMsg", defaultValue = "timeout::72h")
    public static String APIPrimaryStorageMigrateVolumeMsg;
    @GlobalProperty(name="ApiTimeout.org.zstack.storage.migration.backup.APIBackupStorageMigrateImageMsg", defaultValue = "timeout::72h")
    public static String APIBackupStorageMigrateImageMsg;
    @GlobalProperty(name="ApiTimeout.org.zstack.storage.migration.primary.APIPrimaryStorageMigrateVmMsg", defaultValue = "timeout::72h")
    public static String APIPrimaryStorageMigrateVmMsg;

    @GlobalProperty(name="ApiTimeout.org.zstack.header.storage.volume.backup.APICreateVolumeBackupMsg", defaultValue = "timeout::24h")
    public static String APICreateVolumeBackupMsg;
    @GlobalProperty(name="ApiTimeout.org.zstack.header.storage.volume.backup.APICreateVmBackupMsg", defaultValue = "timeout::36h")
    public static String APICreateVmBackupMsg;
}
