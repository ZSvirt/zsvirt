package org.zstack.header.storage.primary;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @apiResult api event for message :ref:`APIDeletePrimaryStorageMsg`
 * @example {
 * "org.zstack.header.storage.primary.APIDeletePrimaryStorageEvent": {
 * "success": true
 * }
 * }
 * @since 0.1.0
 */
@RestResponse
public class APIDeletePrimaryStorageEvent extends APIEvent {

    public APIDeletePrimaryStorageEvent(String apiId) {
        super(apiId);
    }

    public APIDeletePrimaryStorageEvent() {
        super(null);
    }

 
    public static APIDeletePrimaryStorageEvent __example__() {
        APIDeletePrimaryStorageEvent event = new APIDeletePrimaryStorageEvent();

        return event;
    }

}
