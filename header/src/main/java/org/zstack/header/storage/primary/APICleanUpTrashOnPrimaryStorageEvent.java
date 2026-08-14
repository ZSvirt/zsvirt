package org.zstack.header.storage.primary;

import org.zstack.header.core.trash.CleanTrashResult;
import org.zstack.header.core.trash.TrashCleanupResult;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

/**
 * Created by mingjian.deng on 2018/12/10.
 */
@RestResponse(fieldsTo = {"all"})
public class APICleanUpTrashOnPrimaryStorageEvent extends APIEvent {
    private CleanTrashResult result;
    private List<TrashCleanupResult> results;

    public CleanTrashResult getResult() {
        return result;
    }

    public void setResult(CleanTrashResult result) {
        this.result = result;
    }

    public List<TrashCleanupResult> getResults() {
        return results;
    }

    public void setResults(List<TrashCleanupResult> results) {
        this.results = results;
    }

    public APICleanUpTrashOnPrimaryStorageEvent(String apiId) {
        super(apiId);
    }

    public APICleanUpTrashOnPrimaryStorageEvent() {
        super(null);
    }

    public static APICleanUpTrashOnPrimaryStorageEvent __example__() {
        APICleanUpTrashOnPrimaryStorageEvent event = new APICleanUpTrashOnPrimaryStorageEvent();
        CleanTrashResult cleaned = new CleanTrashResult();
        String uuid = uuid();
        cleaned.setResourceUuids(Collections.singletonList(uuid));
        cleaned.setSize(1024000L);

        event.setResult(cleaned);
        event.setResults(Collections.singletonList(new TrashCleanupResult(uuid, 1, 1024000L)));

        return event;
    }
}