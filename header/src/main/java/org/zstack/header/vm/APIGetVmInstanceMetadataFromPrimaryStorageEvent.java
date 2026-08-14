package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"all"})
public class APIGetVmInstanceMetadataFromPrimaryStorageEvent extends APIEvent {
    private String metadata;

    public APIGetVmInstanceMetadataFromPrimaryStorageEvent() {
        super(null);
    }

    public APIGetVmInstanceMetadataFromPrimaryStorageEvent(String apiId) {
        super(apiId);
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public static APIGetVmInstanceMetadataFromPrimaryStorageEvent __example__() {
        APIGetVmInstanceMetadataFromPrimaryStorageEvent evt = new APIGetVmInstanceMetadataFromPrimaryStorageEvent();
        return evt;
    }
}
