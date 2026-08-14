package org.zstack.header.host;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceInventory;

import java.util.Arrays;
import java.util.List;

@RestResponse(fieldsTo = {"all"})
public class APIGetCandidateNetworkInterfacesReply extends APIReply {
    List<String> slaveNames;
    List<HostNetworkInterfaceInventory> candidateNics;

    public List<String> getSlaveNames() {
        return slaveNames;
    }

    public void setSlaveNames(List<String> slaveNames) {
        this.slaveNames = slaveNames;
    }

    public List<HostNetworkInterfaceInventory> getCandidateNics() {
        return candidateNics;
    }

    public void setCandidateNics(List<HostNetworkInterfaceInventory> candidateNics) {
        this.candidateNics = candidateNics;
    }

    public static APIGetCandidateNetworkInterfacesReply __example__() {
        APIGetCandidateNetworkInterfacesReply reply = new APIGetCandidateNetworkInterfacesReply();

        List <String> slaves = Arrays.asList("eth0", "eth1");
        reply.setSlaveNames(slaves);

        return reply;
    }
}
