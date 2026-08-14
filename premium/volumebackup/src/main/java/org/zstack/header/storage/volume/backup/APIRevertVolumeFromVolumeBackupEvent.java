package org.zstack.header.storage.volume.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIRevertVolumeFromVolumeBackupEvent extends APIEvent {
    public APIRevertVolumeFromVolumeBackupEvent(String apiId) {
        super(apiId);
    }

    public APIRevertVolumeFromVolumeBackupEvent() {
        super(null);
    }

    public static APIRevertVolumeFromVolumeBackupEvent __example__() {
        return new APIRevertVolumeFromVolumeBackupEvent();
    }
}
