package org.zstack.storage.backup.imagestore;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by david on 2/25/17.
 */
@RestResponse(allTo = "gcResult")
public class APIReclaimSpaceFromImageStoreEvent extends APIEvent {
    private ImageStoreGcResult gcResult;

    public APIReclaimSpaceFromImageStoreEvent() {
        super(null);
    }

    public APIReclaimSpaceFromImageStoreEvent(String apiId) {
        super(apiId);
    }

    public ImageStoreGcResult getGcResult() {
        return gcResult;
    }

    public void setGcResult(ImageStoreGcResult gcResult) {
        this.gcResult = gcResult;
    }

    public static APIReclaimSpaceFromImageStoreEvent __example__() {
        APIReclaimSpaceFromImageStoreEvent evt = new APIReclaimSpaceFromImageStoreEvent();
        ImageStoreGcResult res = new ImageStoreGcResult();
        res.setFreedSpaceInBytes(4096L);
        evt.setGcResult(res);
        evt.setSuccess(true);
        return evt;
    }
}
