package org.zstack.appliancevm;

import com.google.gson.annotations.SerializedName;
import org.zstack.core.ansible.SyncTimeRequestedDeployArguments;

public class ApplianceVmDeployArguments extends SyncTimeRequestedDeployArguments {
    @SerializedName("pkg_appliancevm")
    private String packageName = ApplianceVmGlobalProperty.AGENT_PACKAGE_NAME;

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public String getPackageName() {
        return packageName;
    }
}
