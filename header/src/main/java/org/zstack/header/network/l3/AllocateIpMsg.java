package org.zstack.header.network.l3;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.utils.network.IPv6Constants;

public class AllocateIpMsg extends NeedReplyMessage implements L3NetworkMessage, IpAllocateMessage {
    private String allocateStrategy;
    private String l3NetworkUuid;
    private String requiredIp;
    private String excludedIp;
    private boolean duplicatedIpAllowed = false;
    private String ipRangeUuid;
    private int ipVersion = IPv6Constants.IPv4;
    private String netmask;
    private int prefixLength;
    private String gateway;

    public String getRequiredIp() {
        return requiredIp;
    }

    @Override
    public void setRequiredIp(String requiredIp) {
        this.requiredIp = requiredIp;
    }

    @Override
    public String getAllocatorStrategy() {
        return allocateStrategy;
    }

    public void setAllocateStrategy(String allocateStrategy) {
        this.allocateStrategy = allocateStrategy;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public void setDuplicatedIpAllowed(boolean duplicatedIpAllowed) {
        this.duplicatedIpAllowed = duplicatedIpAllowed;
    }

    @Override
    public boolean isDuplicatedIpAllowed() {
        return duplicatedIpAllowed;
    }

    @Override
    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    @Override
    public String getExcludedIp() {
        return excludedIp;
    }

    public void setExcludedIp(String excludedIp) {
        this.excludedIp = excludedIp;
    }

    public String getIpRangeUuid() {
        return ipRangeUuid;
    }

    @Override
    public void setIpRangeUuid(String ipRangeUuid) {
        this.ipRangeUuid = ipRangeUuid;
    }

    public int getIpVersion() {
        return ipVersion;
    }

    public void setIpVersion(int ipVersion) {
        this.ipVersion = ipVersion;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public int getPrefixLength() {
        return prefixLength;
    }

    public void setPrefixLength(int prefixLength) {
        this.prefixLength = prefixLength;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }
}
