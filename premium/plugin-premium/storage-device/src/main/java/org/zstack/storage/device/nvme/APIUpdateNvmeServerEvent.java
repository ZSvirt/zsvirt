package org.zstack.storage.device.nvme;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.storage.device.StorageDeviceState;

@RestResponse(allTo = "inventory")
public class APIUpdateNvmeServerEvent extends APIEvent {
    private NvmeServerInventory inventory;

    public NvmeServerInventory getInventory() {
        return inventory;
    }

    public void setInventory(NvmeServerInventory inventory) {
        this.inventory = inventory;
    }

    public APIUpdateNvmeServerEvent(String apiId) {
        super(apiId);
    }

    public APIUpdateNvmeServerEvent() {
        super(null);
    }

    public static APIUpdateNvmeServerEvent __example__() {
        APIUpdateNvmeServerEvent event = new APIUpdateNvmeServerEvent();

        NvmeServerInventory nvmeServerInventory = new NvmeServerInventory();
        nvmeServerInventory.setUuid(uuid());
        nvmeServerInventory.setName("nvme-server-10.0.0.201");
        nvmeServerInventory.setState(StorageDeviceState.Enabled.toString());
        nvmeServerInventory.setIp("10.0.0.201");
        nvmeServerInventory.setPort(3260);

        event.setInventory(nvmeServerInventory);
        event.setSuccess(true);
        return event;
    }
}
