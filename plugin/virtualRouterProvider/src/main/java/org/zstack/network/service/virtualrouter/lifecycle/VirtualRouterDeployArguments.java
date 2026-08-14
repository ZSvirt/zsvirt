package org.zstack.network.service.virtualrouter.lifecycle;

import com.google.gson.annotations.SerializedName;
import org.zstack.core.ansible.SyncTimeRequestedDeployArguments;
import org.zstack.network.service.virtualrouter.VirtualRouterGlobalProperty;

public class VirtualRouterDeployArguments extends SyncTimeRequestedDeployArguments {
    @SerializedName("pkg_virtualrouter")
    private final String packageName = VirtualRouterGlobalProperty.AGENT_PACKAGE_NAME;

    @Override
    public String getPackageName() {
        return packageName;
    }
}
