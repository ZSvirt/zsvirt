package org.zstack.header.host;

import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VdiPortInfo;

/**
 * Created by frank on 1/25/2016.
 */
public class GetVmConsoleAddressFromHostReply extends MessageReply {
    private String hostIp;
    private String protocol;
    private String path;
    private int port;
    private VdiPortInfo vdiPortInfo;

    public String getHostIp() {
        return hostIp;
    }

    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public VdiPortInfo getVdiPortInfo() {
        return vdiPortInfo;
    }

    public void setVdiPortInfo(VdiPortInfo vdiPortInfo) {
        this.vdiPortInfo = vdiPortInfo;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
