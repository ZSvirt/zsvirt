package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class ConfigSecondaryVmMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    private String vmInstanceUuid;
    private String primaryVmHostIp;
    private Integer nbdServerPort;
    private boolean needPrepareColoConfig;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getPrimaryVmHostIp() {
        return primaryVmHostIp;
    }

    public void setPrimaryVmHostIp(String primaryVmHostIp) {
        this.primaryVmHostIp = primaryVmHostIp;
    }

    public Integer getNbdServerPort() {
        return nbdServerPort;
    }

    public void setNbdServerPort(Integer nbdServerPort) {
        this.nbdServerPort = nbdServerPort;
    }

    public boolean isNeedPrepareColoConfig() {
        return needPrepareColoConfig;
    }

    public void setNeedPrepareColoConfig(boolean needPrepareColoConfig) {
        this.needPrepareColoConfig = needPrepareColoConfig;
    }
}
