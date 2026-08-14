package org.zstack.storage.backup.imagestore;

import org.zstack.core.Platform;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APISetImageStoreBackupStorageQuotaEvent extends APIEvent {
    public APISetImageStoreBackupStorageQuotaEvent(String apiId) {
        super(apiId);
    }

    public APISetImageStoreBackupStorageQuotaEvent() {
        super();
    }

    public static APISetImageStoreBackupStorageQuotaEvent __example__() {
        return new APISetImageStoreBackupStorageQuotaEvent(Platform.getUuid());
    }

}
