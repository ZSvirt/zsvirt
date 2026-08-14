package org.zstack.header.network.l3.datatypes;

/**
 * Created by Qi Le on 2020/4/22
 */
public class IpCapacityData {
    private String resourceUuid;

    private long totalCapacity;

    private long availableCapacity;

    private long usedIpAddressNumber;

    private long ipv4TotalCapacity;

    private long ipv4AvailableCapacity;

    private long ipv4UsedIpAddressNumber;

    private long ipv6TotalCapacity;

    private long ipv6AvailableCapacity;

    private long ipv6UsedIpAddressNumber;

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public long getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public long getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(long availableCapacity) {
        this.availableCapacity = availableCapacity;
    }

    public long getUsedIpAddressNumber() {
        return usedIpAddressNumber;
    }

    public void setUsedIpAddressNumber(long usedIpAddressNumber) {
        this.usedIpAddressNumber = usedIpAddressNumber;
    }

    public long getIpv4TotalCapacity() { return ipv4TotalCapacity; }

    public void setIpv4TotalCapacity(long ipv4TotalCapacity) { this.ipv4TotalCapacity = ipv4TotalCapacity; }

    public long getIpv4AvailableCapacity() { return ipv4AvailableCapacity; }

    public void setIpv4AvailableCapacity(long ipv4AvailableCapacity) { this.ipv4AvailableCapacity = ipv4AvailableCapacity; }

    public long getIpv4UsedIpAddressNumber() { return ipv4UsedIpAddressNumber; }

    public void setIpv4UsedIpAddressNumber(long ipv4UsedIpAddressNumber) { this.ipv4UsedIpAddressNumber = ipv4UsedIpAddressNumber; }

    public long getIpv6TotalCapacity() { return ipv6TotalCapacity; }

    public void setIpv6TotalCapacity(long ipv6TotalCapacity) { this.ipv6TotalCapacity = ipv6TotalCapacity; }

    public long getIpv6AvailableCapacity() { return ipv6AvailableCapacity; }

    public void setIpv6AvailableCapacity(long ipv6AvailableCapacity) { this.ipv6AvailableCapacity = ipv6AvailableCapacity; }

    public long getIpv6UsedIpAddressNumber() { return ipv6UsedIpAddressNumber; }

    public void setIpv6UsedIpAddressNumber(long ipv6UsedIpAddressNumber) { this.ipv6UsedIpAddressNumber = ipv6UsedIpAddressNumber; }
}
