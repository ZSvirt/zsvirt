package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by mingjian.deng on 2019/11/25.
 */
public class ScanVmPortMsg extends NeedReplyMessage implements HostMessage {
    private String ip;
    private String brName;
    private int port;
    private String hostUuid;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getBrName() {
        return brName;
    }

    public void setBrName(String brName) {
        this.brName = brName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
