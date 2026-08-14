package org.zstack.header.vm;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

@RestResponse(fieldsTo = {"all"})
public class APIGetVmDnsReply extends APIReply {
    private List<VmDnsInventory> vmDnsList;

    private List<String> dnsList;

    public List<VmDnsInventory> getVmDnsList() {
        return vmDnsList;
    }

    public void setVmDnsList(List<VmDnsInventory> vmDnsList) {
        this.vmDnsList = vmDnsList;
    }

    public List<String> getDnsList() {
        return dnsList;
    }

    public void setDnsList(List<String> dnsList) {
        this.dnsList = dnsList;
    }

    public static APIGetVmDnsReply __example__() {
        APIGetVmDnsReply reply = new APIGetVmDnsReply();
        reply.setDnsList(Collections.singletonList("8.8.8.8"));
        return reply;
    }
}
