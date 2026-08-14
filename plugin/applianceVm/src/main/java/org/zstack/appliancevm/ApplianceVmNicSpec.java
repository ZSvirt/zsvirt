package org.zstack.appliancevm;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class ApplianceVmNicSpec implements Serializable {
    private String l3NetworkUuid;
    private String ip;
    private String netmask;
    private String gateway;
    private String mac;
    private boolean acquireOnNetwork;
    private String allocatorStrategy;
    private String metaData;
    private Map<Integer, String> staticIp = new HashMap<>();
    private boolean allowDuplicatedAddress = false;

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
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

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public boolean isAcquireOnNetwork() {
        return acquireOnNetwork;
    }

    public void setAcquireOnNetwork(boolean acquireOnNetwork) {
        this.acquireOnNetwork = acquireOnNetwork;
    }

    public String getAllocatorStrategy() {
        return allocatorStrategy;
    }

    public void setAllocatorStrategy(String allocatorStrategy) {
        this.allocatorStrategy = allocatorStrategy;
    }

    public String getMetaData() {
        return metaData;
    }

    public void setMetaData(String metaData) {
        this.metaData = metaData;
    }

    public Map<Integer, String> getStaticIp() {
        return staticIp;
    }

    public void setStaticIp(Map<Integer, String> staticIp) {
        this.staticIp = staticIp;
    }

    public boolean isAllowDuplicatedAddress() {
        return allowDuplicatedAddress;
    }

    public void setAllowDuplicatedAddress(boolean allowDuplicatedAddress) {
        this.allowDuplicatedAddress = allowDuplicatedAddress;
    }
}
