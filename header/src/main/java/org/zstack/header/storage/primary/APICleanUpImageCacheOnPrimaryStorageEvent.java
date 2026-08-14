package org.zstack.header.storage.primary;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by xing5 on 2016/7/21.
 */
@RestResponse
public class APICleanUpImageCacheOnPrimaryStorageEvent extends APIEvent {
    public APICleanUpImageCacheOnPrimaryStorageEvent() {
    }

    public APICleanUpImageCacheOnPrimaryStorageEvent(String apiId) {
        super(apiId);
    }
 
    public static APICleanUpImageCacheOnPrimaryStorageEvent __example__() {
        APICleanUpImageCacheOnPrimaryStorageEvent event = new APICleanUpImageCacheOnPrimaryStorageEvent();

        return event;
    }

}
