package org.zstack.network.service.lb;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.vm.VmNicInventory;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

/**
 * Created by xing5 on 2016/11/29.
 */
@RestResponse(allTo = "inventories")
public class APIGetCandidateVmNicsForLoadBalancerReply extends APIReply {
    private List<VmNicInventory> inventories;

    public List<VmNicInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmNicInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIGetCandidateVmNicsForLoadBalancerReply __example__() {
        APIGetCandidateVmNicsForLoadBalancerReply reply = new APIGetCandidateVmNicsForLoadBalancerReply();
        VmNicInventory nic = new VmNicInventory();

        nic.setVmInstanceUuid(uuid());
        nic.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        nic.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        nic.setDeviceId(0);
        nic.setGateway("192.168.1.1");
        nic.setIp("192.168.1.10");
        nic.setL3NetworkUuid(uuid());
        nic.setNetmask("255.255.255.0");
        nic.setMac("00:0c:29:bd:99:fc");
        nic.setHypervisorType("KVM");
        nic.setUsedIpUuid(uuid());
        nic.setUuid(uuid());

        reply.setInventories(Arrays.asList(nic));
        return reply;
    }

}
