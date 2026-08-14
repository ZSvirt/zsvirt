package org.zstack.storage.device.nvme;

import org.zstack.header.message.APIEvent;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestResponse;
import org.zstack.storage.device.StorageDeviceState;

import java.sql.Timestamp;

@RestResponse(allTo = "inventory")
public class APIAddNvmeServerEvent extends APIEvent {
    private NvmeServerInventory inventory;

    public NvmeServerInventory getInventory() {
        return inventory;
    }

    public void setInventory(NvmeServerInventory inventory) {
        this.inventory = inventory;
    }

    public APIAddNvmeServerEvent(String apiId) {
        super(apiId);
    }

    public APIAddNvmeServerEvent() {
        super(null);
    }

    public static org.zstack.storage.device.nvme.APIAddNvmeServerEvent __example__() {
        APIAddNvmeServerEvent event = new APIAddNvmeServerEvent();

        NvmeServerInventory nvmeServerInventory = new NvmeServerInventory();
        nvmeServerInventory.setUuid(uuid());
        nvmeServerInventory.setName("nvme-server-10.0.0.201");
        nvmeServerInventory.setState(StorageDeviceState.Enabled.toString());
        nvmeServerInventory.setIp("10.0.0.201");
        nvmeServerInventory.setPort(3260);
        nvmeServerInventory.setCreateDate(new Timestamp(DocUtils.date));
        nvmeServerInventory.setLastOpDate(new Timestamp(DocUtils.date));

        event.setInventory(nvmeServerInventory);
        event.setSuccess(true);
        return event;
    }
}
