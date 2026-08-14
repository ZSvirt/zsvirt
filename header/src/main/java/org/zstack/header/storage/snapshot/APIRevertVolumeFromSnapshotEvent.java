package org.zstack.header.storage.snapshot;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @apiResult api event for :ref:`APIRevertVolumeFromSnapshotMsg`
 * @category volume snapshot
 * @example {
 * "org.zstack.header.storage.snapshot.APIRevertVolumeFromSnapshotEvent": {
 * "success": true
 * }
 * }
 * @since 0.1.0
 */
@RestResponse
public class APIRevertVolumeFromSnapshotEvent extends APIEvent {
    public APIRevertVolumeFromSnapshotEvent(String apiId) {
        super(apiId);
    }

    public APIRevertVolumeFromSnapshotEvent() {
        super(null);
    }
 
    public static APIRevertVolumeFromSnapshotEvent __example__() {
        APIRevertVolumeFromSnapshotEvent event = new APIRevertVolumeFromSnapshotEvent();

        return event;
    }

}
