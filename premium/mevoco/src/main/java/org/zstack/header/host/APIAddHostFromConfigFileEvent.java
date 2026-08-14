package org.zstack.header.host;

import org.zstack.header.message.APIEvent;
import org.zstack.header.message.NoJsonSchema;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.List;

@RestResponse(fieldsTo = {"all"})
public class APIAddHostFromConfigFileEvent extends APIEvent {
    @NoJsonSchema
    private List<AddHostFromFileResult> results = new ArrayList<>();

    public List<AddHostFromFileResult> getResults() {
        return results;
    }

    public void setResults(List<AddHostFromFileResult> results) {
        this.results = results;
    }

    public APIAddHostFromConfigFileEvent() {
        super();
    }

    public APIAddHostFromConfigFileEvent(String apiId) {
        super(apiId);
    }

    public static APIAddHostFromConfigFileEvent __example__() {
        APIAddHostFromConfigFileEvent evt = new APIAddHostFromConfigFileEvent();
        evt.results = new ArrayList<>();
        AddHostFromFileResult result = new AddHostFromFileResult("127.0.0.1", null);
        evt.results.add(result);
        return evt;
    }
}
