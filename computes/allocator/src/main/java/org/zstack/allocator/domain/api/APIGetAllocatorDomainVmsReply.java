package org.zstack.allocator.domain.api;

import org.zstack.header.message.APIReply;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.vm.VmInstanceVO;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestResponse(fieldsTo = {"all"})
public class APIGetAllocatorDomainVmsReply extends APIReply {
    /**
     * VMs currently in the domain (system tag current value).
     */
    private List<String> currentVms;

    /**
     * VMs whose config value points to this domain and are not in {@link #currentVms}.
     */
    private List<String> nextStartVms;

    /**
     * Running VMs which not in domain, but can be attached to this domain
     */
    private List<String> candidateVms;

    public List<String> getCurrentVms() {
        return currentVms;
    }

    public void setCurrentVms(List<String> currentVms) {
        this.currentVms = currentVms;
    }

    public List<String> getNextStartVms() {
        return nextStartVms;
    }

    public void setNextStartVms(List<String> nextStartVms) {
        this.nextStartVms = nextStartVms;
    }

    public List<String> getCandidateVms() {
        return candidateVms;
    }

    public void setCandidateVms(List<String> candidateVms) {
        this.candidateVms = candidateVms;
    }

    public static APIGetAllocatorDomainVmsReply __example__() {
        APIGetAllocatorDomainVmsReply reply = new APIGetAllocatorDomainVmsReply();
        reply.setCurrentVms(list(DocUtils.createFixedUuid(VmInstanceVO.class)));
        reply.setNextStartVms(new ArrayList<>());
        reply.setCandidateVms(new ArrayList<>());
        return reply;
    }
}
