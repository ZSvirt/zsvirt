package org.zstack.network.securitygroup;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.vm.VmNicInventory;

import java.util.List;

import static java.util.Arrays.asList;

/**
 */
@RestResponse(allTo = "inventories")
public class APIGetCandidateVmNicForSecurityGroupReply extends APIReply {
    private List<VmNicInventory> inventories;

    public List<VmNicInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmNicInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIGetCandidateVmNicForSecurityGroupReply __example__() {
        APIGetCandidateVmNicForSecurityGroupReply reply = new APIGetCandidateVmNicForSecurityGroupReply();
        VmNicInventory vmNic = new VmNicInventory();
        vmNic.setUuid(uuid());
        vmNic.setGateway("192.168.0.1");
        vmNic.setInternalName("eth0");
        vmNic.setDeviceId(0);
        vmNic.setIp("192.168.0.123");
        vmNic.setL3NetworkUuid(uuid());
        vmNic.setMac("fa:ef:34:5c:6c:00");
        vmNic.setHypervisorType("KVM");
        vmNic.setNetmask("255.255.255.0");
        vmNic.setVmInstanceUuid(uuid());
        reply.setInventories(asList(vmNic));
        reply.setSuccess(true);
        return reply;
    }

}
