package org.zstack.header.storage.volume.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIRevertVmFromVmBackupEvent extends APIEvent {
    public APIRevertVmFromVmBackupEvent() {
        super(null);
    }

    public APIRevertVmFromVmBackupEvent(String apiId) {
        super(apiId);
    }

    public static APIRevertVmFromVmBackupEvent __example__() {
        return new APIRevertVmFromVmBackupEvent();
    }
}
