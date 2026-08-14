package org.zstack.header.storage.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"all"})
public class APIExportImageFromBackupStorageEvent extends APIEvent {
    public APIExportImageFromBackupStorageEvent() {
    }

    public APIExportImageFromBackupStorageEvent(String apiId) {
        super(apiId);
    }

    private String imageUrl;
    private String exportMd5Sum;

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getExportMd5Sum() {
        return exportMd5Sum;
    }

    public void setExportMd5Sum(String exportMd5Sum) {
        this.exportMd5Sum = exportMd5Sum;
    }

    public static APIExportImageFromBackupStorageEvent __example__() {
        APIExportImageFromBackupStorageEvent event = new APIExportImageFromBackupStorageEvent();
        event.setImageUrl("http://bs-host-name/path/to/image.qcow2");
        event.setExportMd5Sum("8a239d17b045dea51c4677fa90a42ed8");
        return event;
    }

}
