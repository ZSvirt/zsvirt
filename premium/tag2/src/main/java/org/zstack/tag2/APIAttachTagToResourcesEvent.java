package org.zstack.tag2;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.tag.UserTagInventory;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

@RestResponse(fieldsTo = {"all"})
public class APIAttachTagToResourcesEvent extends APIEvent {
    private List<AttachTagResult> results;

    public APIAttachTagToResourcesEvent(String apiId) {
        super(apiId);
    }

    public APIAttachTagToResourcesEvent() {
        super();
    }

    public List<AttachTagResult> getResults() {
        return results;
    }

    public void setResults(List<AttachTagResult> results) {
        this.results = results;
    }

    public static APIAttachTagToResourcesEvent __example__() {
        APIAttachTagToResourcesEvent evt = new APIAttachTagToResourcesEvent();
        UserTagInventory tag = new UserTagInventory();
        tag.setType("User");
        tag.setResourceType(uuid());
        tag.setResourceType("DiskOfferingVO");
        tag.setTag("for-large-DB");
        tag.setUuid(uuid());
        tag.setTagPatternUuid(uuid());
        tag.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        tag.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        evt.setResults(Collections.singletonList(new AttachTagResult(tag)));
        return evt;
    }
}
