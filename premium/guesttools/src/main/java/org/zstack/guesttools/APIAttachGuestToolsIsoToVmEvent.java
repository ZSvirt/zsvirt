package org.zstack.guesttools;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 2019-09-17.
 */
@RestResponse
public class APIAttachGuestToolsIsoToVmEvent extends APIEvent {
    public APIAttachGuestToolsIsoToVmEvent() {
    }

    public APIAttachGuestToolsIsoToVmEvent(String apiId) {
        super(apiId);
    }

    public static APIAttachGuestToolsIsoToVmEvent __example__() {
        return new APIAttachGuestToolsIsoToVmEvent();
    }
}
