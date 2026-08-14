package org.zstack.header.storage.snapshot;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @apiResult api event for message :ref:`APIDeleteVolumeSnapshotMsg`
 * @category volume snapshot
 * @example {
 * "org.zstack.header.storage.snapshot.APIDeleteVolumeSnapshotEvent": {
 * "success": true
 * }
 * }
 * @since 0.1.0
 */
@RestResponse
public class APIDeleteVolumeSnapshotEvent extends APIEvent {
    public APIDeleteVolumeSnapshotEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteVolumeSnapshotEvent() {
        super(null);
    }
 
    public static APIDeleteVolumeSnapshotEvent __example__() {
        APIDeleteVolumeSnapshotEvent event = new APIDeleteVolumeSnapshotEvent();

        return event;
    }

}
