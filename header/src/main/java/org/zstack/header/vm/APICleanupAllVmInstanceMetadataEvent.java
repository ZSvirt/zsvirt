package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(fieldsTo = {"all"})
public class APICleanupAllVmInstanceMetadataEvent extends APIEvent {
    private List<String> failedPrimaryStorageUuids;

    public APICleanupAllVmInstanceMetadataEvent() {
        super(null);
    }

    public APICleanupAllVmInstanceMetadataEvent(String apiId) {
        super(apiId);
    }

    public List<String> getFailedPrimaryStorageUuids() {
        return failedPrimaryStorageUuids;
    }

    public void setFailedPrimaryStorageUuids(List<String> failedPrimaryStorageUuids) {
        this.failedPrimaryStorageUuids = failedPrimaryStorageUuids;
    }

    public static APICleanupAllVmInstanceMetadataEvent __example__() {
        APICleanupAllVmInstanceMetadataEvent evt = new APICleanupAllVmInstanceMetadataEvent();
        evt.failedPrimaryStorageUuids = java.util.Collections.emptyList();
        return evt;
    }
}
