package org.zstack.header.storage.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by david on 8/31/16.
 */
@RestResponse
public class APIDeleteExportedImageFromBackupStorageEvent extends APIEvent {
    public APIDeleteExportedImageFromBackupStorageEvent() {
    }

    public APIDeleteExportedImageFromBackupStorageEvent(String apiId) {
        super(apiId);
    }
 
    public static APIDeleteExportedImageFromBackupStorageEvent __example__() {
        APIDeleteExportedImageFromBackupStorageEvent event = new APIDeleteExportedImageFromBackupStorageEvent();


        return event;
    }

}
