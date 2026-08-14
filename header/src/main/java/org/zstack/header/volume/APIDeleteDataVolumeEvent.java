package org.zstack.header.volume;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @apiResult api event for message :ref:`APIDeleteDataVolumeMsg`
 * @category volume
 * @example {
 * "org.zstack.header.volume.APIDeleteDataVolumeEvent": {
 * "success": true
 * }
 * }
 * @since 0.1.0
 */
@RestResponse
public class APIDeleteDataVolumeEvent extends APIEvent {
    public APIDeleteDataVolumeEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteDataVolumeEvent() {
        super(null);
    }
 
    public static APIDeleteDataVolumeEvent __example__() {
        APIDeleteDataVolumeEvent event = new APIDeleteDataVolumeEvent();


        return event;
    }

}
