package org.zstack.storage.backup.sftp;

import com.google.gson.annotations.SerializedName;
import org.zstack.core.ansible.SyncTimeRequestedDeployArguments;

public class SftpBackupStorageDeployArguments extends SyncTimeRequestedDeployArguments {
    @SerializedName("pkg_sftpbackupstorage")
    private final String packageName = SftpBackupStorageGlobalProperty.AGENT_PACKAGE_NAME;

    @Override
    public String getPackageName() {
        return packageName;
    }
}
