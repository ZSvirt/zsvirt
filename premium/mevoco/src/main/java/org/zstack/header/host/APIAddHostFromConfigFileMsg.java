package org.zstack.header.host;

import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;

public abstract class APIAddHostFromConfigFileMsg extends APICreateMessage implements AddHostFromConfigFileMessage  {
    /**
     * @desc file content encoded by base64
     */
    @APIParam(nonempty = true)
    private String hostInfo;

    public String getHostInfo() {
        return hostInfo;
    }

    public void setHostInfo(String hostInfo) {
        this.hostInfo = hostInfo;
    }

    public abstract String getHypervisorType();
}
