package org.zstack.zops;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HostBaseInfo {
    private String hostname;
    private int port = 22;
    private Set<HostType> types = new HashSet<>();
    private Set<String> extraIps = new HashSet<>();

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Set<HostType> getTypes() {
        return types;
    }

    public void setTypes(Set<HostType> types) {
        this.types = types;
    }

    public Set<String> getExtraIps() {
        return extraIps;
    }

    public void setExtraIps(Set<String> extraIps) {
        this.extraIps = extraIps;
    }
}
