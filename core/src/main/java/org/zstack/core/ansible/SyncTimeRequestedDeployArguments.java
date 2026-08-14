package org.zstack.core.ansible;

import com.google.gson.annotations.SerializedName;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.header.exception.CloudRuntimeException;

public abstract class SyncTimeRequestedDeployArguments extends AbstractAnsibleAgentDeployArguments {
    @SerializedName("chrony_servers")
    private String chronyServers = initChronyServers();

    public String initChronyServers() {
        // global sync node time disabled
        if (!CoreGlobalProperty.SYNC_NODE_TIME) {
            return null;
        }

        if (CoreGlobalProperty.CHRONY_SERVERS == null || CoreGlobalProperty.CHRONY_SERVERS.isEmpty()) {
            throw new CloudRuntimeException("chrony server not configured!");
        }

        return String.join(",", CoreGlobalProperty.CHRONY_SERVERS);
    }

    public void setChronyServers(String chronyServers) {
        this.chronyServers = chronyServers;
    }

    public String getChronyServers() {
        return chronyServers;
    }
}
