package org.zstack.header.network.l3;

import org.zstack.header.configuration.PythonClass;

@PythonClass
public class UsedIpTO {
    public static final String ACTION_CODE_ADD = "add";
    public static final String ACTION_CODE_REMOVE = "remove";

    private int ipVersion;
    private String ip;
    private String netmask;
    private String gateway;
    private String actionCode;

    public int getIpVersion() {
        return ipVersion;
    }

    public void setIpVersion(int ipVersion) {
        this.ipVersion = ipVersion;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getActionCode() {
        return actionCode;
    }

    public void setActionCode(String actionCode) {
        this.actionCode = actionCode;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("ipVersion: %s,", this.ipVersion));
        sb.append(String.format("ip: %s,", this.ip));
        sb.append(String.format("netmask: %s,", this.netmask));
        sb.append(String.format("gateway: %s,", this.gateway));
        if (this.actionCode != null) {
            sb.append(String.format("action: %s,", this.actionCode));
        }

        return sb.toString();
    }
}
