package org.zstack.storage.ceph.backup;

import com.google.gson.annotations.SerializedName;
import org.zstack.core.ansible.SyncTimeRequestedDeployArguments;
import org.zstack.storage.ceph.CephGlobalProperty;

public class CephBackupStorageDeployArguments extends SyncTimeRequestedDeployArguments {
    @SerializedName("pkg_cephbagent")
    private final String packageName = CephGlobalProperty.BACKUP_STORAGE_PACKAGE_NAME;

    @Override
    public String getPackageName() {
        return packageName;
    }
}
