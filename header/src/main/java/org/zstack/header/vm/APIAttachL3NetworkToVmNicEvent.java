package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIAttachL3NetworkToVmNicEvent extends APIEvent {
    /**
     * @desc see :ref:`VmInstanceInventory`
     */
    private VmNicInventory inventory;

    public APIAttachL3NetworkToVmNicEvent() {
    }

    public APIAttachL3NetworkToVmNicEvent(String apiId) {
        super(apiId);
    }

    public VmNicInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmNicInventory inventory) {
        this.inventory = inventory;
    }

    public static APIAttachL3NetworkToVmNicEvent __example__() {
        APIAttachL3NetworkToVmNicEvent event = new APIAttachL3NetworkToVmNicEvent();

        VmNicInventory nic = new VmNicInventory();
        nic.setVmInstanceUuid(uuid());
        nic.setDeviceId(0);
        nic.setGateway("192.168.1.1");
        nic.setIp("192.168.1.10");
        nic.setL3NetworkUuid(uuid());
        nic.setNetmask("255.255.255.0");
        nic.setMac("00:0c:29:bd:99:fc");
        nic.setUsedIpUuid(uuid());
        nic.setUuid(uuid());

        event.setInventory(nic);


        return event;
    }

}
