package org.zstack.network.service.portforwarding;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.query.ZQLFilterReply;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

/**
 */
@RestResponse(allTo = "inventories")
public class APIGetPortForwardingAttachableVmNicsReply extends APIReply implements ZQLFilterReply {
    private List<VmNicInventory> inventories;

    public List<VmNicInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmNicInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIGetPortForwardingAttachableVmNicsReply __example__() {
        APIGetPortForwardingAttachableVmNicsReply reply = new APIGetPortForwardingAttachableVmNicsReply();
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

    @Override
    public List<String> getInventoryUuids() {
        return inventories.stream().map(VmNicInventory::getUuid).collect(Collectors.toList());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setFilteredInventories(List inventories) {
        this.inventories = (List<VmNicInventory>) inventories;
    }

    @Override
    public String getInventoryName() {
        return VmNicInventory.class.getSimpleName();
    }
}
