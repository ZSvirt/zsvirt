package org.zstack.zsv.storage.api;

import org.zstack.header.configuration.PythonClassInventory;

@PythonClassInventory
public class HostSshParameter {
    private String ip;
    private String username;
    private String password;
    private int port = 22;

    public HostSshParameter() {

    }

    public HostSshParameter(String ip) {
        this.ip = ip;
    }

    public HostSshParameter(String ip, String username, String password, int port) {
        this.ip = ip;
        this.username = username;
        this.password = password;
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean skipCheckPackageInstalled() {
        return username == null || password == null;
    }
}
