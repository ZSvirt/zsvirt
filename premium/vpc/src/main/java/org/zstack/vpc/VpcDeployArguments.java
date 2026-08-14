package org.zstack.vpc;

import com.google.gson.annotations.SerializedName;
import org.zstack.core.ansible.SyncTimeRequestedDeployArguments;
import org.zstack.header.vpc.VpcConstants;

public class VpcDeployArguments extends SyncTimeRequestedDeployArguments {
    @SerializedName("pkg_zsn")
    private final String packageName = VpcConstants.AGENT_PACKAGE_NAME;
    @SerializedName("tmout")
    private Long timeout;

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    @Override
    public String getPackageName() {
        return packageName;
    }
}
