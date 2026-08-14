package org.zstack.network.l2.virtualSwitch.header;

import org.zstack.header.candidate.CandidateResult;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

@RestResponse(fieldsTo = {"all"})
public class APIGetCandidateHostKernelInterfacesReply extends APIReply {
    private List<CandidateResult<HostKernelInterfaceInventory>> results;

    public List<CandidateResult<HostKernelInterfaceInventory>> getResults() {
        return results;
    }

    public void setResults(List<CandidateResult<HostKernelInterfaceInventory>> results) {
        this.results = results;
    }

    public static APIGetCandidateHostKernelInterfacesReply __example__() {
        APIGetCandidateHostKernelInterfacesReply reply = new APIGetCandidateHostKernelInterfacesReply();

        HostKernelInterfaceInventory inv = HostKernelInterfaceInventory.__example__();
        CandidateResult<HostKernelInterfaceInventory> cr = new CandidateResult<>(inv);
        reply.setResults(Collections.singletonList(cr));
        return reply;
    }
}
