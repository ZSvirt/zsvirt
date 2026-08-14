package org.zstack.softwarePackage.header;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"all"})
public class APIGetDirectoryUsageReply extends APIReply {
    private long totalCapacity;
    private long availableCapacity;

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

    public static APIGetDirectoryUsageReply __example__() {
        APIGetDirectoryUsageReply reply = new APIGetDirectoryUsageReply();
        reply.setTotalCapacity(1024);
        reply.setAvailableCapacity(512);
        return reply;
    }
}
