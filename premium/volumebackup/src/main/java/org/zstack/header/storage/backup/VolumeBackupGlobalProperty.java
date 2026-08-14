package org.zstack.header.storage.backup;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;
import org.zstack.header.tag.TagDefinition;

/**
 * Created by MaJin on 2019/5/7.
 */
@GlobalPropertyDefinition
public class VolumeBackupGlobalProperty {
    @GlobalProperty(name="upgradeVolumeBackupHistory", defaultValue = "false")
    public static boolean UPGRADE_VOLUME_BACKUP_HISTORY;

    @GlobalProperty(name="skipTpmVolumeBackup", defaultValue = "true")
    public static boolean SKIP_TPM_VOLUME_BACKUP;
}
