package org.zstack.header.storage.volume.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteVolumeBackupEvent extends APIEvent {
    public APIDeleteVolumeBackupEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteVolumeBackupEvent() {
        super(null);
    }

    public static APIDeleteVolumeBackupEvent __example__() {
        APIDeleteVolumeBackupEvent event = new APIDeleteVolumeBackupEvent();

        return event;
    }
}
