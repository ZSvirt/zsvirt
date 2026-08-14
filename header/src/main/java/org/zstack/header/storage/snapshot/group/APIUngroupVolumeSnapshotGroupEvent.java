package org.zstack.header.storage.snapshot.group;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by MaJin on 2019/7/9.
 */
@RestResponse
public class APIUngroupVolumeSnapshotGroupEvent extends APIEvent {
    public APIUngroupVolumeSnapshotGroupEvent(String apiId) {
        super(apiId);
    }

    public APIUngroupVolumeSnapshotGroupEvent() {
        super();
    }

    public static APIUngroupVolumeSnapshotGroupEvent __example__() {
        return new APIUngroupVolumeSnapshotGroupEvent(uuid());
    }
}
