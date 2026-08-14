package org.zstack.header.host;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

@RestResponse(fieldsTo = {"all"})
public class APIGetCandidateInterfaceVlanIdsReply extends APIReply {
    List<Integer> vlanIds;

    public List<Integer> getVlanIds() {
        return vlanIds;
    }

    public void setVlanIds(List<Integer> vlanIds) {
        this.vlanIds = vlanIds;
    }

    public static APIGetCandidateInterfaceVlanIdsReply __example__() {
        APIGetCandidateInterfaceVlanIdsReply reply = new APIGetCandidateInterfaceVlanIdsReply();

        List <Integer> vlanIds = Arrays.asList(1, 2);
        reply.setVlanIds(vlanIds);

        return reply;
    }
}
