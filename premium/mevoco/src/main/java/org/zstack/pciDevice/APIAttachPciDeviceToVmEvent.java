package org.zstack.pciDevice;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by weiwang on 10/07/2017.
 */
@RestResponse(allTo = "inventory")
public class APIAttachPciDeviceToVmEvent extends APIEvent {
    private PciDeviceInventory inventory;

    public APIAttachPciDeviceToVmEvent() {
    }

    public APIAttachPciDeviceToVmEvent(String apiId) {
        super(apiId);
    }

    public PciDeviceInventory getInventory() {
        return inventory;
    }

    public void setInventory(PciDeviceInventory inventory) {
        this.inventory = inventory;
    }

    public static APIAttachPciDeviceToVmEvent __example__() {
        APIAttachPciDeviceToVmEvent event = new APIAttachPciDeviceToVmEvent();
        PciDeviceInventory inv = new PciDeviceInventory();
        inv.setUuid(uuid());
        inv.setHostUuid(uuid());
        inv.setVmInstanceUuid(uuid());
        inv.setStatus(PciDeviceStatus.Attached);
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
