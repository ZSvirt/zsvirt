package org.zstack.appliancevm;

/**
 */
public class ApplianceVmFirewallRuleTO {
    private String protocol;
    private int startPort;
    private int endPort;
    private String allowCidr;
    private String sourceIp;
    private String destIp;
    private String nicMac;

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public int getStartPort() {
        return startPort;
    }

    public void setStartPort(int startPort) {
        this.startPort = startPort;
    }

    public int getEndPort() {
        return endPort;
    }

    public void setEndPort(int endPort) {
        this.endPort = endPort;
    }

    public String getAllowCidr() {
        return allowCidr;
    }

    public void setAllowCidr(String allowCidr) {
        this.allowCidr = allowCidr;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }

    public String getDestIp() {
        return destIp;
    }

    public void setDestIp(String destIp) {
        this.destIp = destIp;
    }

    public String getNicMac() {
        return nicMac;
    }

    public void setNicMac(String nicMac) {
        this.nicMac = nicMac;
    }
}
