package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * Created by camile on 12/18/2016.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateVmNicMacEvent extends APIEvent {
    private VmNicInventory inventory;

    public APIUpdateVmNicMacEvent() {
    }

    public APIUpdateVmNicMacEvent(String apiId) {
        super(apiId);
    }

    public VmNicInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmNicInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateVmNicMacEvent __example__() {
        APIUpdateVmNicMacEvent event = new APIUpdateVmNicMacEvent();
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
        return event;
    }

}
