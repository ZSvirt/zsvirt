package org.zstack.pciDevice;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by weiwang on 10/07/2017.
 */
@RestResponse(allTo = "inventory")
public class APIUpdatePciDeviceEvent extends APIEvent {
    private PciDeviceInventory inventory;

    public APIUpdatePciDeviceEvent() {
    }

    public APIUpdatePciDeviceEvent(String apiId) {
        super(apiId);
    }

    public PciDeviceInventory getInventory() {
        return inventory;
    }

    public void setInventory(PciDeviceInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdatePciDeviceEvent __example__() {
        APIUpdatePciDeviceEvent event = new APIUpdatePciDeviceEvent();

        PciDeviceInventory inv = new PciDeviceInventory();
        inv.setHostUuid(uuid());
        inv.setVmInstanceUuid(uuid());
        inv.setStatus(PciDeviceStatus.Active);
        inv.setType(PciDeviceType.GPU_Video_Controller);
        inv.setVendorId("10de");
        inv.setDeviceId("0e0f");
        inv.setSubdeviceId("118b");
        inv.setSubvendorId("10de");
        inv.setPciDeviceAddress(new PciDeviceAddress("06:00.1").toString());
        inv.setState(PciDeviceState.Disabled);
        inv.setPassThroughState(PciDevicePassThroughState.Enabled);
        inv.setMetaData(new PciDeviceMetaData("render:Equal:true;anime:Equal:true"));
        inv.setDescription("test pci");

        event.setInventory(inv);
        return event;
    }
}
