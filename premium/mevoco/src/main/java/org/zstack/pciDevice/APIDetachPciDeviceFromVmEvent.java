package org.zstack.pciDevice;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by weiwang on 10/07/2017.
 */
@RestResponse(allTo = "inventory")
public class APIDetachPciDeviceFromVmEvent extends APIEvent {
    private PciDeviceInventory inventory;

    public APIDetachPciDeviceFromVmEvent() {
    }

    public APIDetachPciDeviceFromVmEvent(String apiId) {
        super(apiId);
    }

    public PciDeviceInventory getInventory() {
        return inventory;
    }

    public void setInventory(PciDeviceInventory inventory) {
        this.inventory = inventory;
    }

    public static APIDetachPciDeviceFromVmEvent __example__() {
        APIDetachPciDeviceFromVmEvent event = new APIDetachPciDeviceFromVmEvent();
        PciDeviceInventory inv = new PciDeviceInventory();
        inv.setUuid(uuid());
        inv.setHostUuid(uuid());
        inv.setStatus(PciDeviceStatus.Active);
        inv.setType(PciDeviceType.GPU_Video_Controller);
        inv.setVendorId("10de");
        inv.setDeviceId("0e0f");
        inv.setSubdeviceId("118b");
        inv.setSubvendorId("10de");
        inv.setPciDeviceAddress(new PciDeviceAddress("06:00.1").toString());
        event.setInventory(inv);
        return event;
    }
}
