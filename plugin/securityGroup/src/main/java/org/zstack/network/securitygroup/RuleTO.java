package org.zstack.network.securitygroup;

import java.util.List;

public class RuleTO {
    private int ipVersion;
    private int priority;
    private String type;
    private String remoteGroupUuid;
    private List<String> remoteGroupVmIps;
    private String state;
    private String protocol;
    private String srcIpRange;
    private String dstIpRange;
    private String dstPortRange;
    private String action;

    @Deprecated
    private int startPort;
    @Deprecated
    private int endPort;
    @Deprecated
    private String allowedCidr;

    public int getIpVersion() {
        return ipVersion;
    }

    public void setIpVersion(int ipVersion) {
        this.ipVersion = ipVersion;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getSrcIpRange() {
        return srcIpRange;
    }

    public void setSrcIpRange(String srcIpRange) {
        this.srcIpRange = srcIpRange == null ? null : srcIpRange.replaceAll("\\s+", "");
    }

    public String getDstIpRange() {
        return dstIpRange;
    }

    public void setDstIpRange(String dstIpRange) {
        this.dstIpRange = dstIpRange == null ? null : dstIpRange.replaceAll("\\s+", "");
    }

    public String getDstPortRange() {
        return dstPortRange;
    }

    public void setDstPortRange(String dstPortRange) {
        this.dstPortRange = dstPortRange == null ? null : dstPortRange.replaceAll("\\s+", "");
    }

    public String getProtocol() {
        return protocol;
    }
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    public void setRemoteGroupVmIps(List<String> remoteGroupVmIps) {
        this.remoteGroupVmIps = remoteGroupVmIps;
    }

    public String getRemoteGroupUuid() {
        return remoteGroupUuid;
    }

    public void setRemoteGroupUuid(String remoteGroupUuid) {
        this.remoteGroupUuid = remoteGroupUuid;
    }

    public List<String> getRemoteGroupVmIps() {
        return remoteGroupVmIps;
    }

    @Deprecated
    public int getStartPort() {
        return startPort;
    }
    @Deprecated
    public void setStartPort(int startPort) {
        this.startPort = startPort;
    }
    @Deprecated
    public int getEndPort() {
        return endPort;
    }
    @Deprecated
    public void setEndPort(int endPort) {
        this.endPort = endPort;
    }
    @Deprecated
    public String getAllowedCidr() {
        return allowedCidr;
    }
    @Deprecated
    public void setAllowedCidr(String allowedCidr) {
        this.allowedCidr = allowedCidr;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("ipVersion: %s,", this.ipVersion));
        sb.append(String.format("type: %s,", this.type));
        sb.append(String.format("state: %s,", this.state));
        sb.append(String.format("priority: %s,", this.priority));
        sb.append(String.format("action: %s,", this.action));
        sb.append(String.format("srcIpRange: %s,", this.srcIpRange));
        sb.append(String.format("dstIpRange: %s,", this.dstIpRange));
        sb.append(String.format("protocol: %s,", this.protocol));
        sb.append(String.format("dstPortRange: %s,", this.dstPortRange));
        sb.append(String.format("remoteGroupUuid: %s,", this.remoteGroupUuid));
        sb.append(String.format("remoteGroupVmIps: %s", this.remoteGroupVmIps));
        return sb.toString();
    }
    
    public String toFullString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("ipVersion: %s,", this.ipVersion));
        sb.append(String.format("type: %s,", this.type));
        sb.append(String.format("state: %s,", this.state));
        sb.append(String.format("priority: %s", this.priority));
        sb.append(String.format("action: %s,", this.action));
        sb.append(String.format("srcIpRange: %s,", this.srcIpRange));
        sb.append(String.format("dstIpRange: %s,", this.dstIpRange));
        sb.append(String.format("protocol: %s,", this.protocol));
        sb.append(String.format("dstPortRange: %s,", this.dstPortRange));
        sb.append(String.format("remoteGroupUuid: %s", this.remoteGroupUuid));
        sb.append(String.format("remoteGroupVmIps: %s", this.remoteGroupVmIps));
        return sb.toString();
    }
}
