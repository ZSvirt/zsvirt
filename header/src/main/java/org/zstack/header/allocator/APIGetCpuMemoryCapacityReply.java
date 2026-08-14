package org.zstack.header.allocator;

import org.zstack.header.allocator.datatypes.CpuMemoryCapacityData;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.List;

@RestResponse(fieldsTo = {"all"})
public class APIGetCpuMemoryCapacityReply extends APIReply {
    private long totalCpu;
    private long availableCpu;
    private long totalMemory;
    private long availableMemory;
    private long managedCpuNum;
    private List<CpuMemoryCapacityData> capacityData;
    private String resourceType;

    public long getManagedCpuNum() {
        return managedCpuNum;
    }

    public void setManagedCpuNum(long managedCpuNum) {
        this.managedCpuNum = managedCpuNum;
    }

    public long getAvailableCpu() {
        return availableCpu;
    }

    public void setAvailableCpu(long availableCpu) {
        this.availableCpu = availableCpu;
    }

    public long getAvailableMemory() {
        return availableMemory;
    }

    public void setAvailableMemory(long availableMemory) {
        this.availableMemory = availableMemory;
    }

    public long getTotalCpu() {
        return totalCpu;
    }

    public void setTotalCpu(long totalCpu) {
        this.totalCpu = totalCpu;
    }

    public long getTotalMemory() {
        return totalMemory;
    }

    public void setTotalMemory(long totalMemory) {
        this.totalMemory = totalMemory;
    }

    public List<CpuMemoryCapacityData> getCapacityData() {
        return capacityData;
    }

    public void setCapacityData(List<CpuMemoryCapacityData> capacityData) {
        this.capacityData = capacityData;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public static APIGetCpuMemoryCapacityReply __example__() {
        APIGetCpuMemoryCapacityReply reply = new APIGetCpuMemoryCapacityReply();
        reply.setAvailableCpu(2);
        reply.setAvailableMemory(4);
        reply.setTotalCpu(4);
        reply.setTotalMemory(8);
        reply.setManagedCpuNum(4);
        reply.setResourceType("HostVO");
        List<CpuMemoryCapacityData> dataList = new ArrayList<>();
        reply.setCapacityData(dataList);
        CpuMemoryCapacityData data = new CpuMemoryCapacityData();
        dataList.add(data);
        data.setResourceUuid(uuid());
        data.setAvailableCpu(2);
        data.setAvailableMemory(4);
        data.setTotalCpu(4);
        data.setTotalMemory(8);
        data.setManagedCpuNum(4);
        return reply;
    }

}
