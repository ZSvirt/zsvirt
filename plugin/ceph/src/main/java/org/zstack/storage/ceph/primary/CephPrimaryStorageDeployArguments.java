package org.zstack.storage.ceph.primary;

import com.google.gson.annotations.SerializedName;
import org.zstack.core.ansible.SyncTimeRequestedDeployArguments;
import org.zstack.storage.ceph.CephGlobalProperty;

public class CephPrimaryStorageDeployArguments extends SyncTimeRequestedDeployArguments {
    @SerializedName("pkg_cephpagent")
    private final String packageName = CephGlobalProperty.PRIMARY_STORAGE_PACKAGE_NAME;

    @Override
    public String getPackageName() {
        return packageName;
    }
}
