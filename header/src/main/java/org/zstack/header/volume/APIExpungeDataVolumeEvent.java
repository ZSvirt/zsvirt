package org.zstack.header.volume;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 11/16/2015.
 */
@RestResponse
public class APIExpungeDataVolumeEvent extends APIEvent {
    public APIExpungeDataVolumeEvent() {
    }

    public APIExpungeDataVolumeEvent(String apiId) {
        super(apiId);
    }
 
    public static APIExpungeDataVolumeEvent __example__() {
        APIExpungeDataVolumeEvent event = new APIExpungeDataVolumeEvent();


        return event;
    }

}
