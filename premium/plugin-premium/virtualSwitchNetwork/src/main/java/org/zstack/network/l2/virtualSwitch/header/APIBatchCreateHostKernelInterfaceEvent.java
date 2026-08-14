package org.zstack.network.l2.virtualSwitch.header;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestResponse(fieldsTo = {"all"})
public class APIBatchCreateHostKernelInterfaceEvent extends APIEvent {
    private List<HostKernelInterfaceResult> results;

    public APIBatchCreateHostKernelInterfaceEvent() {
        super(null);
    }

    public APIBatchCreateHostKernelInterfaceEvent(String apiId) {
        super(apiId);
    }

    public List<HostKernelInterfaceResult> getResults() {
        if (results == null) {
            return Collections.emptyList();
        }
        return results;
    }

    public void setResults(List<HostKernelInterfaceResult> results) {
        this.results = results;
    }

    public List<HostKernelInterfaceResult> getResultsWithoutError() {
        if (results == null) {
            return Collections.emptyList();
        }
        return results.stream()
                .filter(it -> it.getError() == null)
                .collect(Collectors.toList());
    }

    public static APIBatchCreateHostKernelInterfaceEvent __example__() {
        APIBatchCreateHostKernelInterfaceEvent evt = new APIBatchCreateHostKernelInterfaceEvent();
        evt.setResults(Collections.singletonList(HostKernelInterfaceResult.__example__()));
        return evt;
    }
}
