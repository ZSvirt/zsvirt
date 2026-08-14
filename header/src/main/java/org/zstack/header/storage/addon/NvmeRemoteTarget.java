package org.zstack.header.storage.addon;

import java.net.URLEncoder;

public class NvmeRemoteTarget extends BlockRemoteTarget {
    private String transport;

    private String nqn;

    private String ip;

    private int port;

    private String hostNqn;

    private String diskId;

    public String getTransport() {
        return transport;
    }

    public void setTransport(String transport) {
        this.transport = transport;
    }

    public String getNqn() {
        return nqn;
    }

    public void setNqn(String nqn) {
        this.nqn = nqn;
    }

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

    public String getHostNqn() {
        return hostNqn;
    }

    public void setHostNqn(String hostNqn) {
        this.hostNqn = hostNqn;
    }

    @Override
    public String getResourceURI() {
        return String.format("nvme://%s@%s:%s/%s/%s", URLEncoder.encode(hostNqn), ip, port, nqn, diskId);
    }

    public String getDiskId() {
        return diskId;
    }

    public void setDiskId(String diskId) {
        this.diskId = diskId;
    }
}
