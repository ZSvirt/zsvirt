package org.zstack.header.storage.primary;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetPrimaryStorageCapacityReply extends APIReply {
    private long totalCapacity;
    private long availableCapacity;
    private long totalPhysicalCapacity;
    private long availablePhysicalCapacity;

    public long getTotalPhysicalCapacity() {
        return totalPhysicalCapacity;
    }

    public void setTotalPhysicalCapacity(long totalPhysicalCapacity) {
        this.totalPhysicalCapacity = totalPhysicalCapacity;
    }

    public long getAvailablePhysicalCapacity() {
        return availablePhysicalCapacity;
    }

    public void setAvailablePhysicalCapacity(long availablePhysicalCapacity) {
        this.availablePhysicalCapacity = availablePhysicalCapacity;
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
 
    public static APIGetPrimaryStorageCapacityReply __example__() {
        APIGetPrimaryStorageCapacityReply reply = new APIGetPrimaryStorageCapacityReply();

        reply.setAvailableCapacity(1024L * 1024L * 928L);
        reply.setAvailablePhysicalCapacity(1024L * 1024L * 928L);
        reply.setTotalCapacity(1024L * 1024L * 1024L);
        reply.setTotalPhysicalCapacity(1024L * 1024L * 1024L);

        return reply;
    }

}
