package org.zstack.header.vm;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 1/25/2016.
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetVmConsoleAddressReply extends APIReply {
    private String hostIp;
    private int port;
    private String path;
    private String protocol;
    private VdiPortInfo vdiPortInfo;

    public String getHostIp() {
        return hostIp;
    }

    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public VdiPortInfo getVdiPortInfo() {
        return vdiPortInfo;
    }

    public void setVdiPortInfo(VdiPortInfo vdiPortInfo) {
        this.vdiPortInfo = vdiPortInfo;
    }

    public static APIGetVmConsoleAddressReply __example__() {
        APIGetVmConsoleAddressReply reply = new APIGetVmConsoleAddressReply();
        reply.hostIp = "192.168.10.100";
        reply.port = 5900;
        reply.protocol = "vnc";
        VdiPortInfo vdiPortInfo = new VdiPortInfo();
        vdiPortInfo.setVncPort(5900);
        reply.setVdiPortInfo(vdiPortInfo);
        return reply;
    }

}
