package org.zstack.header.sriov;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIChangeVfNicHaStateEvent extends APIEvent {
    private VmVfNicInventory inventory;

    public APIChangeVfNicHaStateEvent() {
    }

    public APIChangeVfNicHaStateEvent(String apiId) {
        super(apiId);
    }

    public VmVfNicInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmVfNicInventory inventory) {
        this.inventory = inventory;
    }

    public static APIChangeVfNicHaStateEvent __example__() {
        APIChangeVfNicHaStateEvent evt = new APIChangeVfNicHaStateEvent();
        VmVfNicInventory nic = new VmVfNicInventory();
        nic.setUuid(uuid());
        nic.setVmInstanceUuid(uuid());
        nic.setL3NetworkUuid(uuid());
        nic.setUsedIpUuid(uuid());
        nic.setDeviceId(0);
        nic.setMac("00:0c:29:bd:99:fc");
        nic.setIp("192.168.1.2");
        nic.setNetmask("255.255.255.0");
        nic.setGateway("192.168.1.1");
        nic.setHypervisorType("KVM");
        nic.setType(VmVfNicConstant.VIRTUAL_FUNCTION_TYPE);
        nic.setPciDeviceUuid(uuid());
        nic.setHaState("Enabled");
        evt.setInventory(nic);
        return evt;
    }
}
