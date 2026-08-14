package org.zstack.header.storage.snapshot;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

@RestResponse(fieldsTo = {"all"})
public class APIBatchDeleteVolumeSnapshotEvent extends APIEvent {
    public APIBatchDeleteVolumeSnapshotEvent(String apiId) {
        super(apiId);
    }

    public APIBatchDeleteVolumeSnapshotEvent() {
        super(null);
    }

    private List<BatchDeleteVolumeSnapshotStruct> results;

    public List<BatchDeleteVolumeSnapshotStruct> getResults() {
        return results;
    }

    public void setResults(List<BatchDeleteVolumeSnapshotStruct> results) {
        this.results = results;
    }

    public static APIBatchDeleteVolumeSnapshotEvent __example__() {
        APIBatchDeleteVolumeSnapshotEvent event = new APIBatchDeleteVolumeSnapshotEvent();

        BatchDeleteVolumeSnapshotStruct r1 = new BatchDeleteVolumeSnapshotStruct();
        r1.setSnapshotUuid(uuid());
        r1.setSuccess(true);
        event.setResults(Arrays.asList(r1));
        return event;
    }
}
