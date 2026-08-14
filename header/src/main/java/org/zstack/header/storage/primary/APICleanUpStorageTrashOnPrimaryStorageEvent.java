package org.zstack.header.storage.primary;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.List;
import java.util.Map;

@RestResponse(fieldsTo = {"all"})
public class APICleanUpStorageTrashOnPrimaryStorageEvent extends APIEvent {
    private Map<String, List<String>> result;
    private Integer total;

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public Map<String, List<String>> getResult() {
        return result;
    }

    public void setResult(Map<String, List<String>> result) {
        this.result = result;
    }

    public APICleanUpStorageTrashOnPrimaryStorageEvent(String apiId) {
        super(apiId);
    }

    public APICleanUpStorageTrashOnPrimaryStorageEvent() {
    }
}
