package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import java.sql.Timestamp;

@RestResponse(allTo = "inventory")
public class APICreateVmNicEvent extends APIEvent {
    /**
     * @desc see :ref:`VmInstanceInventory`
     */
    private VmNicInventory inventory;

    public APICreateVmNicEvent() {
        super(null);
    }

    public APICreateVmNicEvent(String apiId) {
        super(apiId);
    }

    public VmNicInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmNicInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateVmNicEvent __example__() {
        APICreateVmNicEvent evt = new APICreateVmNicEvent();

        VmNicInventory nic = new VmNicInventory();
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

        evt.setInventory(nic);

        return evt;
    }
}
