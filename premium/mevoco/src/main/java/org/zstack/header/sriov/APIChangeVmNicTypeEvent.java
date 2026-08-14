package org.zstack.header.sriov;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.vm.VmNicInventory;

/**
 * Created by GuoYi on 12/9/19.
 */
@RestResponse(allTo = "inventory")
public class APIChangeVmNicTypeEvent extends APIEvent {
    private VmNicInventory inventory;

    public APIChangeVmNicTypeEvent() {
    }

    public APIChangeVmNicTypeEvent(String apiId) {
        super(apiId);
    }

    public VmNicInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmNicInventory inventory) {
        this.inventory = inventory;
    }

    public static APIChangeVmNicTypeEvent __example__() {
        APIChangeVmNicTypeEvent evt = new APIChangeVmNicTypeEvent();
        VmNicInventory nic = new VmNicInventory();
        nic.setUuid(uuid());
        nic.setVmInstanceUuid(uuid());
        nic.setL3NetworkUuid(uuid());
        nic.setUsedIpUuid(uuid());
        nic.setDeviceId(0);
        nic.setMac("00:0c:29:bd:99:fc");
        nic.setIp("192.168.1.10");
        nic.setNetmask("255.255.255.0");
        nic.setGateway("192.168.1.1");
        nic.setHypervisorType("KVM");
        nic.setType(VmVfNicConstant.VIRTUAL_FUNCTION_TYPE);
        evt.setInventory(nic);
        return evt;
    }
}
