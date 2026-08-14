package org.zstack.network.l2.virtualSwitch.header;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.rest.SDK;

@PythonClassInventory
@SDK
public class HostKernelInterfaceStruct {
    private String name;
    private String description;
    private String hostUuid;
    private String ip;
    private String netmask;
    private String ip6;
    private Integer ipv6Prefix;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
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

    public String getIp6() {
        return ip6;
    }

    public void setIp6(String ip6) {
        this.ip6 = ip6;
    }

    public Integer getIpv6Prefix() {
        return ipv6Prefix;
    }

    public void setIpv6Prefix(Integer ipv6Prefix) {
        this.ipv6Prefix = ipv6Prefix;
    }
}
