package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(fieldsTo = {"all"})
public class APICleanupVmInstanceMetadataEvent extends APIEvent {
    private Integer totalCleaned;
    private Integer totalFailed;
    private List<String> failedVmUuids;

    public APICleanupVmInstanceMetadataEvent() {
        super(null);
    }

    public APICleanupVmInstanceMetadataEvent(String apiId) {
        super(apiId);
    }

    public Integer getTotalCleaned() {
        return totalCleaned;
    }

    public void setTotalCleaned(Integer totalCleaned) {
        this.totalCleaned = totalCleaned;
    }

    public Integer getTotalFailed() {
        return totalFailed;
    }

    public void setTotalFailed(Integer totalFailed) {
        this.totalFailed = totalFailed;
    }

    public List<String> getFailedVmUuids() {
        return failedVmUuids;
    }

    public void setFailedVmUuids(List<String> failedVmUuids) {
        this.failedVmUuids = failedVmUuids;
    }

    public static APICleanupVmInstanceMetadataEvent __example__() {
        APICleanupVmInstanceMetadataEvent evt = new APICleanupVmInstanceMetadataEvent();
        evt.totalCleaned = 5;
        evt.totalFailed = 0;
        evt.failedVmUuids = java.util.Collections.emptyList();
        return evt;
    }
}
