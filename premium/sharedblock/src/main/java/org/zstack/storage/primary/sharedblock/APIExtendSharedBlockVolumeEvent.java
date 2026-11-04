package org.zstack.storage.primary.sharedblock;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIExtendSharedBlockVolumeEvent extends APIEvent {
    public APIExtendSharedBlockVolumeEvent() {
    }

    public APIExtendSharedBlockVolumeEvent(String apiId) {
        super(apiId);
    }

    public static APIExtendSharedBlockVolumeEvent __example__() {
        return new APIExtendSharedBlockVolumeEvent();
    }
}
