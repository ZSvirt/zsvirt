package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

public class RegisterColoPrimaryCheckMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    private String vmInstanceUuid;
    private Integer heartbeatPort;
    private String targetHostIp;
    private boolean coloPrimary;
    private Integer redirectNum;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public Integer getHeartbeatPort() {
        return heartbeatPort;
    }

    public void setHeartbeatPort(Integer heartbeatPort) {
        this.heartbeatPort = heartbeatPort;
    }

    public String getTargetHostIp() {
        return targetHostIp;
    }

    public void setTargetHostIp(String targetHostIp) {
        this.targetHostIp = targetHostIp;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public boolean isColoPrimary() {
        return coloPrimary;
    }

    public void setColoPrimary(boolean coloPrimary) {
        this.coloPrimary = coloPrimary;
    }

    public Integer getRedirectNum() {
        return redirectNum;
    }

    public void setRedirectNum(Integer redirectNum) {
        this.redirectNum = redirectNum;
    }
}

