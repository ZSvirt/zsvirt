package org.zstack.header.network.l3;

import org.zstack.header.message.APIReply;
import org.zstack.header.network.l3.datatypes.IpCapacityData;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.List;


/**
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetIpAddressCapacityReply extends APIReply {
    private long totalCapacity;
    private long availableCapacity;
    private long usedIpAddressNumber;
    private long ipv4TotalCapacity;
    private long ipv4AvailableCapacity;
    private long ipv4UsedIpAddressNumber;
    private long ipv6TotalCapacity;
    private long ipv6AvailableCapacity;
    private long ipv6UsedIpAddressNumber;
    private List<IpCapacityData> capacityData;
    private String resourceType;

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

    public List<IpCapacityData> getCapacityData() {
        return capacityData;
    }

    public void setCapacityData(List<IpCapacityData> capacityData) {
        this.capacityData = capacityData;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public long getIpv6TotalCapacity() {
        return ipv6TotalCapacity;
    }

    public void setIpv6TotalCapacity(long ipv6TotalCapacity) {
        this.ipv6TotalCapacity = ipv6TotalCapacity;
    }

    public long getIpv6AvailableCapacity() {
        return ipv6AvailableCapacity;
    }

    public void setIpv6AvailableCapacity(long ipv6AvailableCapacity) {
        this.ipv6AvailableCapacity = ipv6AvailableCapacity;
    }

    public long getIpv6UsedIpAddressNumber() {
        return ipv6UsedIpAddressNumber;
    }

    public void setIpv6UsedIpAddressNumber(long ipv6UsedIpAddressNumber) {
        this.ipv6UsedIpAddressNumber = ipv6UsedIpAddressNumber;
    }

    public long getIpv4TotalCapacity() {
        return ipv4TotalCapacity;
    }

    public void setIpv4TotalCapacity(long ipv4TotalCapacity) {
        this.ipv4TotalCapacity = ipv4TotalCapacity;
    }

    public long getIpv4AvailableCapacity() {
        return ipv4AvailableCapacity;
    }

    public void setIpv4AvailableCapacity(long ipv4AvailableCapacity) {
        this.ipv4AvailableCapacity = ipv4AvailableCapacity;
    }

    public long getIpv4UsedIpAddressNumber() {
        return ipv4UsedIpAddressNumber;
    }

    public void setIpv4UsedIpAddressNumber(long ipv4UsedIpAddressNumber) {
        this.ipv4UsedIpAddressNumber = ipv4UsedIpAddressNumber;
    }

    public static APIGetIpAddressCapacityReply __example__() {
        APIGetIpAddressCapacityReply reply = new APIGetIpAddressCapacityReply();
        reply.setAvailableCapacity(15L);
        reply.setTotalCapacity(20L);
        reply.setUsedIpAddressNumber(5L);
        reply.setResourceType("L3NetworkVO");
        List<IpCapacityData> capacityData = new ArrayList<>();
        reply.setCapacityData(capacityData);
        IpCapacityData data = new IpCapacityData();
        capacityData.add(data);
        data.setAvailableCapacity(15L);
        data.setTotalCapacity(20L);
        data.setUsedIpAddressNumber(5L);
        data.setResourceUuid(uuid());
        return reply;
    }

}
