package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 7/13/2015.
 */
@RestResponse
public class APIRevokeResourceSharingEvent extends APIEvent {
    public APIRevokeResourceSharingEvent() {
    }

    public APIRevokeResourceSharingEvent(String apiId) {
        super(apiId);
    }
 
    public static APIRevokeResourceSharingEvent __example__() {
        APIRevokeResourceSharingEvent event = new APIRevokeResourceSharingEvent();


        return event;
    }

}
