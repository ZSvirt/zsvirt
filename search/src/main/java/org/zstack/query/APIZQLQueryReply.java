package org.zstack.query;

import org.zstack.header.log.MaskSensitiveInfo;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIReply;
import org.zstack.header.message.NoJsonSchema;
import org.zstack.header.rest.RestResponse;
import org.zstack.zql.ZQLQueryReturn;

import java.util.List;

@RestResponse(allTo = "results")
@MaskSensitiveInfo
public class APIZQLQueryReply extends APIReply {
    @NoJsonSchema
    @NoLogging(behavior = NoLogging.Behavior.Auto)
    private List<ZQLQueryReturn> results;

    public static APIZQLQueryReply __example__() {
        APIZQLQueryReply ret = new APIZQLQueryReply();
        return ret;
    }

    public List<ZQLQueryReturn> getResults() {
        return results;
    }

    public void setResults(List<ZQLQueryReturn> results) {
        this.results = results;
    }
}
