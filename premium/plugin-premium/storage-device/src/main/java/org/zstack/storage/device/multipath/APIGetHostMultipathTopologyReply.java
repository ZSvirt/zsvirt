package org.zstack.storage.device.multipath;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;
import java.util.Map;

@RestResponse(allTo = "results")
public class APIGetHostMultipathTopologyReply extends APIReply {
    private List<MultipathTopologyStruct> results;

    public List<MultipathTopologyStruct> getResults() {
        return results;
    }

    public void setResults(List<MultipathTopologyStruct> results) {
        this.results = results;
    }
}
