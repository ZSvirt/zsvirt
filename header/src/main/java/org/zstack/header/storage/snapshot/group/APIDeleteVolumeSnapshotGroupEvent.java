package org.zstack.header.storage.snapshot.group;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by MaJin on 2019/7/9.
 */
@RestResponse(allTo = "results")
public class APIDeleteVolumeSnapshotGroupEvent extends APIEvent {
    private List<DeleteSnapshotGroupResult> results;

    public APIDeleteVolumeSnapshotGroupEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteVolumeSnapshotGroupEvent() {
        super();
    }

    public List<DeleteSnapshotGroupResult> getResults() {
        return results;
    }

    public void setResults(List<DeleteSnapshotGroupResult> results) {
        this.results = results;
    }

    public static APIDeleteVolumeSnapshotGroupEvent __example__() {
        APIDeleteVolumeSnapshotGroupEvent event = new APIDeleteVolumeSnapshotGroupEvent(uuid());
        DeleteSnapshotGroupResult result = new DeleteSnapshotGroupResult();
        result.setError(new ErrorCode("SYS.1001", "internal error"));
        event.setResults(Collections.singletonList(result));
        return event;
    }
}
