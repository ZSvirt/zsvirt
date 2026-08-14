package org.zstack.header.vm.cdrom;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

@RestResponse(allTo = "inventory")
public class APISetVmInstanceDefaultCdRomEvent extends APIEvent {
    private VmCdRomInventory inventory;

    public APISetVmInstanceDefaultCdRomEvent() {
        super(null);
    }

    public APISetVmInstanceDefaultCdRomEvent(String apiId) {
        super(apiId);
    }

    public VmCdRomInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmCdRomInventory inventory) {
        this.inventory = inventory;
    }

    public static APISetVmInstanceDefaultCdRomEvent __example__() {
        APISetVmInstanceDefaultCdRomEvent evt = new APISetVmInstanceDefaultCdRomEvent();

        VmCdRomInventory inventory = new VmCdRomInventory();
        inventory.setName("cd-1");
        inventory.setVmInstanceUuid(uuid());
        inventory.setIsoUuid(uuid());
        inventory.setDeviceId(0);
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        evt.setInventory(inventory);

        return evt;
    }
}
