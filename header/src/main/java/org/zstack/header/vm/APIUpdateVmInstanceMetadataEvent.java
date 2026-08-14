package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"all"})
public class APIUpdateVmInstanceMetadataEvent extends APIEvent {
    public APIUpdateVmInstanceMetadataEvent() {
        super(null);
    }

    public APIUpdateVmInstanceMetadataEvent(String apiId) {
        super(apiId);
    }

    public static APIUpdateVmInstanceMetadataEvent __example__() {
        return new APIUpdateVmInstanceMetadataEvent();
    }
}
