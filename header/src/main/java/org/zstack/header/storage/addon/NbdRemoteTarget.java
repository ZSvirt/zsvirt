package org.zstack.header.storage.addon;

public class NbdRemoteTarget extends BlockRemoteTarget {
    private String ip;
    private int port;
    private String installPath;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String getInstallPath() {
        return installPath;
    }

    @Override
    public String getResourceURI() {
        return String.format("nbd://%s:%s", ip, port);
    }
}
