package org.zstack.tag2;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.tag.UserTagInventory;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

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
        evt.setResults(list(new AttachTagResult(UserTagInventory.__example__())));
        return evt;
    }
}
