package org.zstack.core.errorcode;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by mingjian.deng on 2018/12/21.
 */
@RestResponse(fieldsTo = {"results"})
public class APICheckElaborationContentReply extends APIReply {
    private List<ElaborationCheckResult> results = new ArrayList<>();

    public List<ElaborationCheckResult> getResults() {
        return results;
    }

    public void setResults(List<ElaborationCheckResult> results) {
        this.results = results;
    }

    public static APICheckElaborationContentReply __example__() {
        APICheckElaborationContentReply reply = new APICheckElaborationContentReply();
        ElaborationCheckResult result = new ElaborationCheckResult();
        result.setFileName("/tmp/Host1.json");
        result.setReason(ElaborationFailedReason.InValidJsonArraySchema.toString());

        reply.setResults(asList(result));
        return reply;
    }
}
