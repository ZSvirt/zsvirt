package org.zstack.header.volume;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIValidateVolumeSnapshotChainEvent extends APIEvent {
    public APIValidateVolumeSnapshotChainEvent() {
        super(null);
    }

    public APIValidateVolumeSnapshotChainEvent(String apiId) {
        super(apiId);
    }

    public static APIValidateVolumeSnapshotChainEvent __example__() {
        APIValidateVolumeSnapshotChainEvent event = new APIValidateVolumeSnapshotChainEvent();
        return event;
    }
}
