package org.zstack.storage.zbs;

import com.google.gson.annotations.SerializedName;
import org.zstack.core.ansible.SyncTimeRequestedDeployArguments;

/**
 * @author Xingwei Yu
 * @date 2024/3/27 17:12
 */
public class ZbsPrimaryStorageDeployArguments extends SyncTimeRequestedDeployArguments {
    @SerializedName("pkg_zbspagent")
    private final String packageName = ZbsGlobalProperty.PRIMARY_STORAGE_PACKAGE_NAME;

    @Override
    public String getPackageName() {
        return packageName;
    }
}
