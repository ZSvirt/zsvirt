package org.zstack.header.storage.volume.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteVmBackupEvent extends APIEvent {
    public APIDeleteVmBackupEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteVmBackupEvent() {
        super(null);
    }

    public static APIDeleteVmBackupEvent __example__() {
        APIDeleteVmBackupEvent event = new APIDeleteVmBackupEvent();

        return event;
    }
}
