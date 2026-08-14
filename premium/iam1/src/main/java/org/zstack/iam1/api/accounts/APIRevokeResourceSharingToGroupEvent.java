package org.zstack.iam1.api.accounts;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIRevokeResourceSharingToGroupEvent extends APIEvent {
    public APIRevokeResourceSharingToGroupEvent(String apiId) {
        super(apiId);
    }

    public APIRevokeResourceSharingToGroupEvent() {
        super(null);
    }

    public static APIRevokeResourceSharingToGroupEvent __example__() {
        return new APIRevokeResourceSharingToGroupEvent();
    }
}
