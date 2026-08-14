package org.zstack.baremetal.pxeserver;

import com.google.gson.annotations.SerializedName;
import org.zstack.core.ansible.SyncTimeRequestedDeployArguments;
import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerGlobalProperty;

public class BaremetalDeployArguments extends SyncTimeRequestedDeployArguments {
    @SerializedName("pkg_baremetalpxeserver")
    private final String packageName = BaremetalPxeServerGlobalProperty.AGENT_PACKAGE_NAME;
    @SerializedName("update_packages")
    private String updatePackages;

    public String getUpdatePackages() {
        return updatePackages;
    }

    public void setUpdatePackages(String updatePackages) {
        this.updatePackages = updatePackages;
    }

    @Override
    public String getPackageName() {
        return packageName;
    }
}
