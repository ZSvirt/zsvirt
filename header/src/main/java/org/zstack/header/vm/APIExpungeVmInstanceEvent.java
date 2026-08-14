package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 11/12/2015.
 */
@RestResponse
public class APIExpungeVmInstanceEvent extends APIEvent {
    public APIExpungeVmInstanceEvent() {
    }

    public APIExpungeVmInstanceEvent(String apiId) {
        super(apiId);
    }
 
    public static APIExpungeVmInstanceEvent __example__() {
        APIExpungeVmInstanceEvent event = new APIExpungeVmInstanceEvent();
        return event;
    }

}
