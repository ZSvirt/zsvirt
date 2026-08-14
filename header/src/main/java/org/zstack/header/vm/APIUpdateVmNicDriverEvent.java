package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUpdateVmNicDriverEvent extends APIEvent {
    /**
     * @desc see :ref:`VmInstanceInventory`
     */
    private VmNicInventory inventory;

    public APIUpdateVmNicDriverEvent() {
    }

    public APIUpdateVmNicDriverEvent(String apiId) {
        super(apiId);
    }

    public VmNicInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmNicInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateVmNicDriverEvent __example__() {
        APIUpdateVmNicDriverEvent event = new APIUpdateVmNicDriverEvent();

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
        nic.setDriverType(VmNicConstant.NIC_DRIVER_TYPE_E1000);

        event.setInventory(nic);


        return event;
    }

}
